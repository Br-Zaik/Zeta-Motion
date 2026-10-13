package com.zeta.fondo.engine

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import com.zeta.fondo.model.EffectTimeline
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Dibuja la imagen animada con el shader principal. Se usa igual en la vista
 * previa (GLSurfaceView) y en la exportación (superficie del codificador o FBO),
 * así lo que ves es lo que se exporta.
 * Debe crearse y usarse siempre en el hilo que tiene el contexto OpenGL.
 *
 * v36: los parámetros de los efectos viven en un Uniform Buffer (bloque std140
 * "ZetaParams"). Antes eran ~250 uniforms sueltos, al límite de las GPU Adreno/Mali
 * económicas (GL_MAX_FRAGMENT_UNIFORM_VECTORS = 224-256), lo que podía impedir el
 * enlace del shader en algunos teléfonos. Ahora el límite es el tamaño del bloque
 * (mínimo garantizado 16 KB; usamos ~3-4 KB) y se sube con una sola llamada.
 * Las máscaras base (proteger, zona azul y zona verde) se leen del mismo atlas
 * 2D-array que las máscaras propias: menos memoria y una sola subida por trazo.
 */
class FondoGl(private val vertSrc: String, private val fragSrc: String) {

    private var program = 0
    private var vao = 0
    private var vbo = 0
    private val tex = IntArray(3) // 0 imagen, 1 flujo, 2 atlas de máscaras (2D array)
    private val locs = HashMap<String, Int>()
    private var uploadedParams: RenderParams? = null
    private var maskBuf: ByteBuffer? = null
    private var atlasW = 0
    private var atlasH = 0

    // ---- Uniform buffer de parámetros
    private var ubo = 0
    private var blockSize = 0
    private var block: ByteBuffer = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
    private var blockDirty = true
    private val blockOffsets = HashMap<String, IntArray>() // nombre -> [offset, stride, esEntero]

    private var lastCharAspect = Float.NaN
    private val charTex = IntArray(1)
    var charAspect = 1f
        private set

    var texW = 1
        private set
    var texH = 1
        private set

    fun init() {
        clearErrors()
        program = buildProgram(vertSrc, fragSrc)

        val ids = IntArray(1)
        GLES30.glGenVertexArrays(1, ids, 0)
        vao = ids[0]
        GLES30.glGenBuffers(1, ids, 0)
        vbo = ids[0]
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        val quad = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val fb = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        fb.put(quad).position(0)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, fb, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        initParamBlock()

        GLES30.glGenTextures(3, tex, 0)

        // Texturas por defecto (vacías) para que el shader siempre tenga algo válido
        val px = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        px.put(byteArrayOf(20, 24, 48, -1)).position(0)
        setupTex(0, 0, false)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, 1, 1, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, px)
        uploadFlowRaw(1, 1, ShortArray(2))
        prepareMasks(4, 4)
        GLES30.glGenTextures(1, charTex, 0)
        uploadCharacter(null)
        checkGl("init")
    }

    private fun initParamBlock() {
        val index = GLES30.glGetUniformBlockIndex(program, BLOCK_NAME)
        if (index == GL_INVALID_INDEX) throw RuntimeException("El shader no declara el bloque $BLOCK_NAME")
        val size = IntArray(1)
        GLES30.glGetActiveUniformBlockiv(program, index, GLES30.GL_UNIFORM_BLOCK_DATA_SIZE, size, 0)
        blockSize = size[0].coerceAtLeast(16)
        val maxBlock = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAX_UNIFORM_BLOCK_SIZE, maxBlock, 0)
        if (maxBlock[0] in 1 until blockSize) {
            throw RuntimeException("La GPU admite bloques de ${maxBlock[0]} bytes y el shader necesita $blockSize")
        }
        GLES30.glUniformBlockBinding(program, index, BLOCK_BINDING)
        block = ByteBuffer.allocateDirect(blockSize).order(ByteOrder.nativeOrder())
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        ubo = ids[0]
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, ubo)
        GLES30.glBufferData(GLES30.GL_UNIFORM_BUFFER, blockSize, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBufferBase(GLES30.GL_UNIFORM_BUFFER, BLOCK_BINDING, ubo)
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, 0)
        blockOffsets.clear()
        blockDirty = true
    }

    /** [offset en bytes, salto entre elementos de array, 1 si es int] o null si el shader no lo usa. */
    private fun member(name: String): IntArray? {
        blockOffsets[name]?.let { return if (it[0] < 0) null else it }
        val indices = IntArray(1)
        GLES30.glGetUniformIndices(program, arrayOf(name), indices, 0)
        val info = if (indices[0] == GL_INVALID_INDEX) intArrayOf(-1, 0, 0) else {
            val out = IntArray(1)
            GLES30.glGetActiveUniformsiv(program, 1, indices, 0, GLES30.GL_UNIFORM_OFFSET, out, 0)
            val offset = out[0]
            GLES30.glGetActiveUniformsiv(program, 1, indices, 0, GLES30.GL_UNIFORM_ARRAY_STRIDE, out, 0)
            val stride = out[0]
            GLES30.glGetActiveUniformsiv(program, 1, indices, 0, GLES30.GL_UNIFORM_TYPE, out, 0)
            intArrayOf(offset, stride, if (out[0] == GLES30.GL_INT) 1 else 0)
        }
        blockOffsets[name] = info
        return if (info[0] < 0) null else info
    }

    /** Escribe un valor del bloque. Acepta "uNombre" y "uNombre[i]". */
    private fun putFloats(name: String, a: FloatArray, count: Int = a.size) {
        var base = name
        var element = 0
        val bracket = name.indexOf('[')
        if (bracket > 0) {
            base = name.substring(0, bracket)
            element = name.substring(bracket + 1, name.length - 1).toIntOrNull() ?: 0
        }
        val m = member(base) ?: member("$base[0]") ?: return
        val start = m[0] + element * m[1]
        for (i in 0 until count.coerceAtMost(a.size)) {
            if (start + i * 4 + 4 > blockSize) break
            block.putFloat(start + i * 4, a[i])
        }
        blockDirty = true
    }

    private fun put4(name: String, x: Float, y: Float, z: Float, w: Float) {
        val m = member(name) ?: return
        val o = m[0]
        if (o + 16 > blockSize) return
        block.putFloat(o, x); block.putFloat(o + 4, y); block.putFloat(o + 8, z); block.putFloat(o + 12, w)
        blockDirty = true
    }

    private fun put3(name: String, x: Float, y: Float, z: Float) {
        val m = member(name) ?: return
        val o = m[0]
        if (o + 12 > blockSize) return
        block.putFloat(o, x); block.putFloat(o + 4, y); block.putFloat(o + 8, z)
        blockDirty = true
    }

    private fun put2(name: String, x: Float, y: Float) {
        val m = member(name) ?: return
        if (m[0] + 8 > blockSize) return
        block.putFloat(m[0], x); block.putFloat(m[0] + 4, y)
        blockDirty = true
    }

    private fun put1(name: String, x: Float) {
        val m = member(name) ?: return
        if (m[0] + 4 > blockSize) return
        block.putFloat(m[0], x)
        blockDirty = true
    }

    private fun putInt(name: String, v: Int) {
        val m = member(name) ?: return
        if (m[0] + 4 > blockSize) return
        if (m[2] == 1) block.putInt(m[0], v) else block.putFloat(m[0], v.toFloat())
        blockDirty = true
    }

    private fun flushBlock() {
        if (!blockDirty) return
        block.position(0)
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, ubo)
        GLES30.glBufferSubData(GLES30.GL_UNIFORM_BUFFER, 0, blockSize, block)
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, 0)
        blockDirty = false
    }

    private fun setupTex(unit: Int, slot: Int, mipmap: Boolean) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[slot])
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER,
            if (mipmap) GLES30.GL_LINEAR_MIPMAP_LINEAR else GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    }

    /** Sube la imagen a su resolución real, con mipmaps para reducir sin serrucho. */
    fun uploadImage(bmp: Bitmap) {
        clearErrors()
        setupTex(0, 0, true)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        texW = bmp.width
        texH = bmp.height
        checkGl("uploadImage")
    }

    /** Personaje PNG con transparencia para el efecto Aparición (null = vacío). */
    fun uploadCharacter(bmp: Bitmap?) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE6)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, charTex[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        if (bmp == null || bmp.isRecycled) {
            val z = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, 1, 1, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, z)
            charAspect = 1f
        } else {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
            charAspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1)
        }
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    }

    fun uploadFlow(d: FlowField.Data) = uploadFlowRaw(d.w, d.h, d.halfs)

    private fun uploadFlowRaw(w: Int, h: Int, halfs: ShortArray) {
        val buf = ByteBuffer.allocateDirect(halfs.size * 2).order(ByteOrder.nativeOrder())
        buf.asShortBuffer().put(halfs)
        buf.position(0)
        setupTex(1, 1, false)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RG16F, w, h, 0, GLES30.GL_RG, GLES30.GL_HALF_FLOAT, buf)
        checkGl("uploadFlow")
    }

    /** Reserva todas las máscaras en una única textura-array GLES3, sin aumentar las unidades de sampler. */
    fun prepareMasks(w: Int, h: Int): Boolean {
        if (w == atlasW && h == atlasH) return false
        atlasW = w
        atlasH = h
        GLES30.glActiveTexture(GLES30.GL_TEXTURE5)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, tex[2])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        val empty = ByteBuffer.allocateDirect(w * h * MaskLayers.MAX_MASKS)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage3D(GLES30.GL_TEXTURE_2D_ARRAY, 0, GLES30.GL_R8,
            w, h, MaskLayers.MAX_MASKS, 0, GLES30.GL_RED, GLES30.GL_UNSIGNED_BYTE, empty)
        checkGl("prepareMasks")
        return true
    }

    /**
     * Sube una capa ALPHA_8 al atlas. Con [rowFrom]/[rowTo] solo se envían las filas
     * que cambió el pincel (antes se reenviaba la capa completa dos veces por trazo).
     */
    fun uploadMask(layer: Int, bmp: Bitmap, rowFrom: Int = 0, rowTo: Int = bmp.height) {
        val size = bmp.rowBytes * bmp.height
        var buf = maskBuf
        if (buf == null || buf.capacity() < size) {
            buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
            maskBuf = buf
        }
        buf!!.position(0)
        buf.limit(size)
        bmp.copyPixelsToBuffer(buf)
        prepareMasks(bmp.width, bmp.height)
        val y0 = rowFrom.coerceIn(0, bmp.height)
        val y1 = rowTo.coerceIn(y0, bmp.height)
        if (y1 <= y0) return
        buf.position(y0 * bmp.rowBytes)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, bmp.rowBytes)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE5)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, tex[2])
        GLES30.glTexSubImage3D(GLES30.GL_TEXTURE_2D_ARRAY, 0, 0, y0, layer,
            bmp.width, y1 - y0, 1, GLES30.GL_RED, GLES30.GL_UNSIGNED_BYTE, buf)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0)
        buf.position(0)
        checkGl("uploadMask")
    }

    private fun loc(name: String): Int = locs.getOrPut(name) { GLES30.glGetUniformLocation(program, name) }

    private fun studioName(id: String) = id.replaceFirstChar { it.uppercaseChar() }

    /**
     * Aplica solo los cambios que dependen del tiempo de la línea de tiempo.
     * Todos los valores se escriben en la copia local del bloque y se suben juntos.
     */
    private fun applyTimeline(p: RenderParams, t: Float) {
        if (p.timeline.isEmpty()) return
        var boltLocal = -1f
        var boltSeed = 0f
        var impactLocal = -1f

        for ((id, clips) in p.timeline) {
            val sample = EffectTimeline.sample(clips, t, p.loopSec)
            val gate = sample.gate
            when (id) {
                "flow" -> put3("uFlow3", p.flow[0] * gate, p.flow[1], p.flow[2])
                "drift" -> put2("uDrift", p.drift[0] * gate, p.drift[1] * gate)
                "ripple" -> put3("uRipple", p.ripple[0] * gate, p.ripple[1], p.ripple[2])
                "glint" -> put3("uGlint", p.glint[0] * gate, p.glint[1], p.glint[2])
                "sway" -> put3("uSway", p.sway[0] * gate, p.sway[1], p.sway[2])
                "heat" -> put3("uHeat", p.heat[0] * gate, p.heat[1], p.heat[2])
                "rain" -> put4("uRain", p.rain[0] * gate, p.rain[1], p.rain[2], p.rain[3])
                "snow" -> put4("uSnow", p.snow[0] * gate, p.snow[1], p.snow[2], p.snow[3])
                "sakura" -> put4("uSakura", p.sakura[0] * gate, p.sakura[1], p.sakura[2], p.sakura[3])
                "stars" -> put4("uStars", p.stars[0] * gate, p.stars[1], p.stars[2], p.stars[3])
                "flies" -> put4("uFlies", p.flies[0] * gate, p.flies[1], p.flies[2], p.flies[3])
                "fog" -> put4("uFog", p.fog[0] * gate, p.fog[1], p.fog[2], p.fog[3])
                "rays" -> put4("uRays", p.rays[0] * gate, p.rays[1], p.rays[2], p.rays[3])
                "energy" -> put4("uEnergy", p.energy[0] * gate, p.energy[1], p.energy[2], p.energy[3])
                "speed" -> put4("uSpeed", p.speed[0] * gate, p.speed[1], p.speed[2], p.speed[3])
                "cam" -> put3("uCam", p.cam[0] * gate, p.cam[1] * gate, p.cam[2])
                "parallax" -> p.studio["uParallax"]?.let { put4("uParallax", it[0] * gate, it[1], it[2], it[3]) }
                "mesh" -> p.studio["uMesh"]?.let { put4("uMesh", it[0] * gate, it[1], it[2], it[3]) }
                "natural" -> p.studio["uNatural"]?.let { put4("uNatural", it[0] * gate, it[1], it[2], it[3]) }
                "zonemotion" -> p.studio["uZoneMotion"]?.let { put4("uZoneMotion", it[0] * gate, it[1], it[2], it[3]) }
                "sky" -> p.studio["uStudioSky"]?.let { put4("uStudioSky", it[0] * gate, it[1], it[2], it[3]) }
                "water" -> p.studio["uStudioWater"]?.let { put4("uStudioWater", it[0] * gate, it[1], it[2], it[3]) }
                "dispersion" -> p.studio["uStudioDispersion"]?.let { put4("uStudioDispersion", it[0] * gate, it[1], it[2], it[3]) }
                "bolt" -> p.studio["uBolt"]?.let {
                    put4("uBolt", it[0] * gate, it[1], it[2], it[3])
                    if (sample.clip != null && gate > 0f) {
                        boltLocal = sample.localSec
                        boltSeed = (sample.clip.uid % 997L).toFloat()
                    }
                }
                "gust" -> p.studio["uGust"]?.let {
                    // La posición y duración las gobierna cada bloque, no un único start/dur global.
                    put4("uGust", it[0] * gate, 0f, 1f, it[3])
                }
                "tornado" -> p.studio["uTornado"]?.let { put4("uTornado", it[0] * gate, it[1], it[2], it[3]) }
                "appear" -> p.studio["uAppear"]?.let {
                    val c = sample.clip
                    val start = c?.let { clip -> (clip.startSec / p.loopSec).coerceIn(0f, .999f) } ?: it[1]
                    val baseTransition = it[2]
                    val trans = c?.let { clip -> minOf(clip.durationSec / p.loopSec, baseTransition).coerceAtLeast(.02f) } ?: baseTransition
                    put4("uAppear", it[0] * gate, start, trans, it[3])
                }
                "clouds3" -> p.studio["uClouds3"]?.let { put4("uClouds3", it[0] * gate, it[1], it[2], it[3]) }
                "shore" -> p.studio["uShore"]?.let { put4("uShore", it[0] * gate, it[1], it[2], it[3]) }
                "sunpath" -> p.studio["uSunpath"]?.let { put4("uSunpath", it[0] * gate, it[1], it[2], it[3]) }
                "impact" -> p.studio["uImpact"]?.let {
                    put4("uImpact", it[0] * gate, it[1], it[2], 1f)
                    if (sample.clip != null && gate > 0f) impactLocal = sample.localSec
                }
                else -> p.extra[id]?.let { put4("uFx${studioName(id)}", it[0] * gate, it[1], it[2], it[3]) }
            }
        }

        if (p.timeline.containsKey("bolt") || p.timeline.containsKey("impact")) {
            put4("uLoopInfo", p.loopSec, boltLocal, impactLocal, boltSeed)
        }
    }

    /** Escribe todos los parámetros (solo cuando cambia el objeto RenderParams). */
    private fun writeParams(p: RenderParams) {
        p.studio.forEach { (name, value) -> putFloats(name, value) }
        put1("uAspect", p.aspect)
        put1("uMono", p.mono)
        put1("uPixelSnap", p.snap)
        put3("uFlow3", p.flow[0], p.flow[1], p.flow[2])
        put2("uDrift", p.drift[0], p.drift[1])
        put3("uRipple", p.ripple[0], p.ripple[1], p.ripple[2])
        put3("uGlint", p.glint[0], p.glint[1], p.glint[2])
        put3("uSway", p.sway[0], p.sway[1], p.sway[2])
        put3("uHeat", p.heat[0], p.heat[1], p.heat[2])
        putFloats("uRain", p.rain)
        putFloats("uSnow", p.snow)
        putFloats("uSakura", p.sakura)
        putFloats("uStars", p.stars)
        putFloats("uFlies", p.flies)
        putFloats("uFog", p.fog)
        putFloats("uRays", p.rays)
        putFloats("uEnergy", p.energy)
        putFloats("uSpeed", p.speed)
        put3("uCam", p.cam[0], p.cam[1], p.cam[2])
        p.motion.forEach { (id, value) -> putFloats("uMotion${studioName(id)}", value) }
        p.extra.forEach { (id, value) -> putFloats("uFx${studioName(id)}", value) }
        p.copyMotion.forEach { (id, value) -> putFloats("uMotion${studioName(id)}Copy", value) }
        p.copyExtra.forEach { (id, value) -> putFloats("uFx${studioName(id)}Copy", value) }
        p.copyMasks.forEach { (id, mask) -> putInt("uMask${studioName(id)}Copy", mask) }
        p.effectMasks.forEach { (id, mask) -> putInt("uMask${studioName(id)}", mask) }
        blockDirty = true
    }

    /**
     * view = [offsetX, offsetY, scaleX, scaleY] en coordenadas de imagen.
     * flipY: para leer píxeles con glReadPixels (PNG). rotate: vídeo vertical guardado en horizontal.
     */
    fun draw(
        p: RenderParams, t: Float, vw: Int, vh: Int, view: FloatArray,
        flipY: Boolean, rotate: Boolean, showMask: Boolean, overlayOnly: Boolean,
        activeMask: Int = -1,
        vx0: Int = 0,
        vy0: Int = 0
    ) {
        GLES30.glViewport(vx0, vy0, vw, vh)
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[1])
        GLES30.glUniform1i(loc("uImage"), 0)
        GLES30.glUniform1i(loc("uFlow"), 1)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE5)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, tex[2])
        GLES30.glUniform1i(loc("uMasks"), 5)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE6)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, charTex[0])
        GLES30.glUniform1i(loc("uCharacter"), 6)

        GLES30.glUniform2f(loc("uViewOffset"), view[0], view[1])
        GLES30.glUniform2f(loc("uViewScale"), view[2], view[3])
        GLES30.glUniform1f(loc("uFlipY"), if (flipY) 1f else 0f)
        GLES30.glUniform1f(loc("uRotate"), if (rotate) 1f else 0f)
        GLES30.glUniform1f(loc("uT"), t)
        GLES30.glUniform2f(loc("uTexSize"), texW.toFloat(), texH.toFloat())
        GLES30.glUniform1f(loc("uShowMask"), if (showMask) 1f else 0f)
        GLES30.glUniform1i(loc("uActiveMask"), activeMask)
        GLES30.glUniform1f(loc("uOverlayOnly"), if (overlayOnly) 1f else 0f)
        // Los parámetros son inmutables; el bloque completo solo se reescribe cuando
        // cambia RenderParams. La línea de tiempo modifica unos pocos valores por cuadro.
        if (uploadedParams !== p) {
            writeParams(p)
            lastCharAspect = Float.NaN
            uploadedParams = p
        }
        applyTimeline(p, t)
        if (lastCharAspect != charAspect) { put1("uCharAspect", charAspect); lastCharAspect = charAspect }
        flushBlock()
        GLES30.glBindBufferBase(GLES30.GL_UNIFORM_BUFFER, BLOCK_BINDING, ubo)

        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    fun maxTextureSize(): Int {
        val a = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, a, 0)
        return if (a[0] > 0) a[0] else 2048
    }

    fun release() {
        if (program != 0) GLES30.glDeleteProgram(program)
        GLES30.glDeleteTextures(3, tex, 0)
        GLES30.glDeleteTextures(1, charTex, 0)
        GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
        if (ubo != 0) GLES30.glDeleteBuffers(1, intArrayOf(ubo), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
        program = 0
        ubo = 0
    }

    private fun clearErrors() {
        var guard = 0
        while (GLES30.glGetError() != GLES30.GL_NO_ERROR && guard < 16) guard++
    }

    private fun checkGl(where: String) {
        val e = GLES30.glGetError()
        if (e != GLES30.GL_NO_ERROR) {
            throw RuntimeException("OpenGL error 0x${Integer.toHexString(e)} en $where")
        }
    }

    companion object {
        const val BLOCK_NAME = "ZetaParams"
        private const val BLOCK_BINDING = 0
        /** GL_INVALID_INDEX (0xFFFFFFFF) visto como Int de Java. */
        private const val GL_INVALID_INDEX = -1

        fun buildProgram(v: String, f: String): Int {
            val vs = compile(GLES30.GL_VERTEX_SHADER, v)
            val fs = compile(GLES30.GL_FRAGMENT_SHADER, f)
            val prog = GLES30.glCreateProgram()
            GLES30.glAttachShader(prog, vs)
            GLES30.glAttachShader(prog, fs)
            GLES30.glLinkProgram(prog)
            val ok = IntArray(1)
            GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, ok, 0)
            GLES30.glDeleteShader(vs)
            GLES30.glDeleteShader(fs)
            if (ok[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(prog)
                GLES30.glDeleteProgram(prog)
                throw RuntimeException("Error al enlazar shaders: $log")
            }
            return prog
        }

        private fun compile(type: Int, src: String): Int {
            val s = GLES30.glCreateShader(type)
            GLES30.glShaderSource(s, src)
            GLES30.glCompileShader(s)
            val ok = IntArray(1)
            GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(s)
                GLES30.glDeleteShader(s)
                val kind = if (type == GLES30.GL_VERTEX_SHADER) "vértices" else "fragmentos"
                throw RuntimeException("Error en el shader de $kind: $log")
            }
            return s
        }
    }
}
