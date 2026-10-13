package com.zeta.fondo.engine

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.zeta.fondo.model.Effects
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PreviewRenderer(
    private val hub: RenderHub,
    private val vertSrc: String,
    private val fragSrc: String
) : GLSurfaceView.Renderer {

    private var gl: FondoGl? = null
    private var vw = 1
    private var vh = 1
    private var lastImage: Bitmap? = null
    private var lastFlow: FlowField.Data? = null
    private var lastChar: Bitmap? = null
    private var charUploaded = false
    private var lastMasks: MaskLayers? = null
    private val maskVer = IntArray(MaskLayers.MAX_MASKS) { -1 }
    private val view = FloatArray(4)
    private var failed = false
    private var firstFrameSent = false
    private var maxTextureSize = 2048
    // Parámetros neutros: comparación Original/Animado sin tocar el proyecto ni las máscaras.
    private val originalParams = RenderParams.from(
        Effects.defaults().mapValues { (key, value) ->
            if (key.endsWith(".on")) 0f else value
        }, 1f)

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // Contexto nuevo: hay que volver a subir todo
        lastImage = null
        lastFlow = null
        lastChar = null
        charUploaded = false
        lastMasks = null
        maskVer.fill(-1)
        failed = false
        firstFrameSent = false
        val maxTex = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, maxTex, 0)
        maxTextureSize = maxTex[0].coerceAtLeast(2048)
        hub.onFramePending?.invoke()
        gl = try {
            FondoGl(vertSrc, fragSrc).also { it.init() }
        } catch (t: Throwable) {
            failed = true
            hub.onError?.invoke(t.message ?: t.toString())
            null
        }
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        vw = width
        vh = height
    }

    override fun onDrawFrame(unused: GL10?) {
        val g = gl
        if (g == null || failed || !hub.renderEnabled) {
            GLES30.glClearColor(0.08f, 0.09f, 0.19f, 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            return
        }
        try {
            val img = hub.image
            if (img != null && img !== lastImage && !img.isRecycled) {
                if (img.width <= maxTextureSize && img.height <= maxTextureSize) {
                    g.uploadImage(img)
                } else {
                    // Seguridad para GPUs antiguas: la app sigue funcionando aunque el
                    // dispositivo tenga un GL_MAX_TEXTURE_SIZE menor que la preview adaptativa.
                    val longSide = maxOf(img.width, img.height).toFloat()
                    val k = maxTextureSize / longSide
                    val w = maxOf(1, (img.width * k).toInt())
                    val h = maxOf(1, (img.height * k).toInt())
                    val safe = Bitmap.createScaledBitmap(img, w, h, true)
                    try { g.uploadImage(safe) } finally { if (safe !== img) safe.recycle() }
                }
                lastImage = img
            }
            val f = hub.flow
            if (f != null && f !== lastFlow) {
                g.uploadFlow(f)
                lastFlow = f
            }
            val ch = hub.character
            if (!charUploaded || ch !== lastChar) {
                g.uploadCharacter(ch)
                lastChar = ch
                charUploaded = true
            }
            val m = hub.masks
            if (m != null) {
                if (m !== lastMasks) {
                    lastMasks = m
                    maskVer.fill(-1)
                }
                synchronized(m.lock) {
                    if (m.count > 0 && g.prepareMasks(m.w, m.h)) maskVer.fill(-1)
                    for (i in m.bitmaps.indices) {
                        if (m.versions[i] != maskVer[i]) {
                            // Solo las filas tocadas por el pincel; una capa nueva o un
                            // contexto recreado se sube completo.
                            val rows = m.takeDirtyRows(i)
                            if (maskVer[i] == -1) g.uploadMask(i, m.bitmaps[i])
                            else if (rows[1] > rows[0]) g.uploadMask(i, m.bitmaps[i], rows[0], rows[1])
                            maskVer[i] = m.versions[i]
                        }
                    }
                }
            }
            if (lastImage == null) {
                GLES30.glClearColor(0.08f, 0.09f, 0.19f, 1f)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                return
            }
            view[0] = hub.viewX
            view[1] = hub.viewY
            view[2] = hub.viewWidth
            view[3] = hub.viewHeight
            // Encuadre centrado dentro de la superficie: cambiar de formato no recrea la vista.
            val a = hub.displayAspect.takeIf { it > 0.01f } ?: (vw.toFloat() / vh.coerceAtLeast(1))
            var dw = vw
            var dh = vh
            if (vw.toFloat() / vh.coerceAtLeast(1) > a) dw = (vh * a).toInt().coerceIn(1, vw)
            else dh = (vw / a).toInt().coerceIn(1, vh)
            g.draw(if (hub.showOriginal) originalParams else hub.params,
                if (hub.showOriginal) 0f else hub.currentT(), dw, dh, view,
                false, false, if (hub.showOriginal) false else hub.showMask,
                false, hub.activeMask, (vw - dw) / 2, (vh - dh) / 2)
            if (!firstFrameSent) {
                firstFrameSent = true
                hub.onFrameReady?.invoke()
            }
        } catch (t: Throwable) {
            failed = true
            hub.onError?.invoke(t.message ?: t.toString())
        }
    }
}
