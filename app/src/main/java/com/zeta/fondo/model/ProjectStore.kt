package com.zeta.fondo.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.AtomicFile
import com.zeta.fondo.engine.BitmapUtil
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ProjectSummary(
    val id: String,
    val name: String,
    val thumb: File,
    val modified: Long,
    val w: Int,
    val h: Int
)

data class ProjectMeta(
    val id: String,
    val name: String,
    val dir: File,
    val imageFile: File,
    val w: Int,
    val h: Int
)

class LoadedProject(
    val meta: ProjectMeta,
    val arrows: List<Arrow>,
    val anchors: List<PointN>,
    val strokes: List<MaskStroke>,
    val settings: Map<String, Float>,
    val export: ExportSettings,
    val maskNames: List<String>,
    val effectClips: List<EffectClip>
)

/**
 * Cada proyecto es una carpeta: source.img (copia EXACTA de tu imagen, sin
 * recomprimir), thumb.jpg y project.json con flechas, máscaras y ajustes.
 */
class ProjectStore(private val ctx: Context) {

    private val root = File(ctx.filesDir, "projects")

    fun list(): List<ProjectSummary> {
        root.mkdirs()
        val dirs = root.listFiles() ?: return emptyList()
        return dirs.filter { it.isDirectory && (File(it, JSON).exists() || File(it, "$JSON.bak").exists()) }.mapNotNull { dir ->
            try {
                val jf = File(dir, JSON)
                val j = JSONObject(AtomicFile(jf).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
                ProjectSummary(
                    dir.name, j.optString("name", dir.name), File(dir, THUMB),
                    jf.lastModified(), j.getInt("w"), j.getInt("h")
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.modified }
    }

    fun create(uri: Uri, loopSec: Int = 6): String {
        root.mkdirs()
        val id = System.currentTimeMillis().toString()
        val dir = File(root, id)
        dir.mkdirs()
        try {
            val cr = ctx.contentResolver
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val opened = cr.openInputStream(uri) ?: throw IOException("No se pudo abrir la imagen")
            opened.use { BitmapFactory.decodeStream(it, null, o) }
            if (o.outWidth <= 0 || o.outHeight <= 0) throw IOException("Ese archivo no es una imagen compatible")

            val img = File(dir, IMAGE)
            var w = o.outWidth
            var h = o.outHeight
            val rotation = readRotation(uri)
            if (rotation == 0) {
                // Copia byte a byte: conserva la calidad y resolución originales
                val input = cr.openInputStream(uri) ?: throw IOException("No se pudo leer la imagen")
                input.use { i -> img.outputStream().buffered(256 * 1024).use { o -> i.copyTo(o, 256 * 1024) } }
            } else {
                val input = cr.openInputStream(uri) ?: throw IOException("No se pudo leer la imagen")
                val full = input.use { BitmapFactory.decodeStream(it) } ?: throw IOException("No se pudo decodificar la imagen")
                val m = Matrix().apply { postRotate(rotation.toFloat()) }
                val rot = Bitmap.createBitmap(full, 0, 0, full.width, full.height, m, true)
                img.outputStream().use { rot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                w = rot.width
                h = rot.height
                if (rot !== full) rot.recycle()
                full.recycle()
            }

            val th = BitmapUtil.decodePreview(img, 480)
            File(dir, THUMB).outputStream().use { th.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            th.recycle()

            // Caché de edición nítida: evita abrir con el thumbnail borroso y luego cambiar
            // a otra textura. 2560 px mantiene líneas de anime nítidas en pantallas móviles sin cargar el 4K
            // original en memoria durante el primer cuadro.
            val editPreview = BitmapUtil.decodePreview(img, 1920)
            File(dir, EDIT_PREVIEW).outputStream().buffered(256 * 1024).use {
                editPreview.compress(Bitmap.CompressFormat.JPEG, 97, it)
            }
            editPreview.recycle()

            val name = "Fondo " + SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
            val j = JSONObject().put("name", name).put("w", w).put("h", h)
                .put("export", ExportSettings(loopSec = loopSec.coerceIn(2, 60)).toJson())
            // También el primer guardado de un proyecto debe resistir cierres abruptos.
            val initial = AtomicFile(File(dir, JSON))
            val output = initial.startWrite()
            try {
                output.write(j.toString().toByteArray(Charsets.UTF_8))
                initial.finishWrite(output)
            } catch (error: Throwable) {
                initial.failWrite(output)
                throw error
            }
            return id
        } catch (e: Throwable) {
            dir.deleteRecursively()
            throw e
        }
    }

    private fun readRotation(uri: Uri): Int = try {
        val input = ctx.contentResolver.openInputStream(uri)
        if (input == null) 0 else input.use { s ->
            when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
    } catch (e: Exception) {
        0
    }


    fun loadOrCreateEditPreview(meta: ProjectMeta, maxSide: Int): Bitmap {
        val cached = File(meta.dir, EDIT_PREVIEW)
        if (cached.isFile) {
            try {
                // Si el teléfono tiene capacidad y se acerca mucho, conserva una
                // preview más nítida. Los móviles de poca RAM siguen usando 1600 px.
                val (cw, ch) = BitmapUtil.bounds(cached)
                val (iw, ih) = BitmapUtil.bounds(meta.imageFile)
                if (maxOf(cw, ch) >= minOf(maxSide, maxOf(iw, ih)) - 16) {
                    return BitmapUtil.decodePreview(cached, maxSide)
                }
            } catch (_: Throwable) {
                cached.delete()
            }
        }
        val bmp = BitmapUtil.decodePreview(meta.imageFile, maxSide)
        // Si es un proyecto antiguo, se crea una vez y los siguientes accesos son inmediatos.
        try {
            val tmp = File(meta.dir, "${EDIT_PREVIEW}.tmp")
            tmp.outputStream().buffered(256 * 1024).use { stream ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 97, stream)
            }
            if (!tmp.renameTo(cached)) tmp.delete()
        } catch (_: Throwable) {
            // La caché es opcional; nunca impedimos abrir el proyecto.
        }
        return bmp
    }

    fun load(id: String): LoadedProject {
        val dir = File(root, id)
        val j = JSONObject(AtomicFile(File(dir, JSON)).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        val w = j.getInt("w")
        val h = j.getInt("h")
        val meta = ProjectMeta(id, j.optString("name", "Fondo"), dir, File(dir, IMAGE), w, h)

        val arrows = ArrayList<Arrow>()
        j.optJSONArray("arrows")?.let { a ->
            for (i in 0 until a.length()) {
                val e = a.getJSONArray(i)
                arrows.add(Arrow(e.f(0), e.f(1), e.f(2), e.f(3)))
            }
        }
        val anchors = ArrayList<PointN>()
        j.optJSONArray("anchors")?.let { a ->
            for (i in 0 until a.length()) {
                val e = a.getJSONArray(i)
                anchors.add(PointN(e.f(0), e.f(1)))
            }
        }
        val strokes = ArrayList<MaskStroke>()
        j.optJSONArray("strokes")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val s = MaskStroke(
                    o.getInt("l"),
                    o.getDouble("r").toFloat(),
                    o.optDouble("f", 0.3).toFloat(),
                    o.optBoolean("e", false),
                    o.optBoolean("fill", false)
                )
                o.optJSONArray("p")?.let { p ->
                    var k = 0
                    while (k + 1 < p.length()) {
                        s.points.add(PointN(p.f(k), p.f(k + 1)))
                        k += 2
                    }
                }
                strokes.add(s)
            }
        }
        val settings = HashMap(Effects.defaults())
        j.optJSONObject("settings")?.let { o ->
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                settings[k] = o.optDouble(k, 0.0).toFloat()
            }
        }
        val export = ExportSettings.fromJson(j.optJSONObject("export"))
        val maskNames = ArrayList<String>()
        j.optJSONArray("maskNames")?.let { arr ->
            for (i in 0 until arr.length()) maskNames.add(arr.optString(i, "Máscara $i"))
        }
        val effectClips = ArrayList<EffectClip>()
        j.optJSONArray("effectClips")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("effectId", "")
                if (id.isBlank()) continue
                effectClips.add(EffectClip(
                    uid = o.optLong("uid", (i + 1).toLong()),
                    effectId = id,
                    startSec = o.optDouble("startSec", 0.0).toFloat(),
                    durationSec = o.optDouble("durationSec", export.loopSec.toDouble()).toFloat(),
                    track = o.optInt("track", -1),
                    strength = o.optDouble("strength", 1.0).toFloat().coerceIn(0f, 2f)
                ))
            }
        }
        return LoadedProject(meta, arrows, anchors, strokes, settings, export, maskNames, effectClips)
    }

    @Synchronized
    fun save(
        meta: ProjectMeta,
        arrows: List<Arrow>,
        anchors: List<PointN>,
        strokes: List<MaskStroke>,
        settings: Map<String, Float>,
        export: ExportSettings,
        maskNames: List<String>,
        effectClips: List<EffectClip> = emptyList()
    ) {
        val j = JSONObject()
            .put("name", meta.name)
            .put("w", meta.w)
            .put("h", meta.h)
        val a = JSONArray()
        for (ar in arrows) a.put(JSONArray().put(r4(ar.x0)).put(r4(ar.y0)).put(r4(ar.x1)).put(r4(ar.y1)))
        j.put("arrows", a)
        val an = JSONArray()
        for (p in anchors) an.put(JSONArray().put(r4(p.x)).put(r4(p.y)))
        j.put("anchors", an)
        val st = JSONArray()
        for (s in strokes) {
            val pts = JSONArray()
            for (p in s.points) {
                pts.put(r4(p.x))
                pts.put(r4(p.y))
            }
            st.put(
                JSONObject()
                    .put("l", s.layer)
                    .put("r", s.radius.toDouble())
                    .put("f", s.feather.toDouble())
                    .put("e", s.erase)
                    .put("fill", s.fill)
                    .put("p", pts)
            )
        }
        j.put("strokes", st)
        j.put("maskNames", JSONArray(maskNames))
        val so = JSONObject()
        for ((k, v) in settings) so.put(k, v.toDouble())
        j.put("settings", so)
        j.put("export", export.toJson())
        val clips = JSONArray()
        for (clip in effectClips) clips.put(JSONObject()
            .put("uid", clip.uid)
            .put("effectId", clip.effectId)
            .put("startSec", clip.startSec.toDouble())
            .put("durationSec", clip.durationSec.toDouble())
            .put("track", clip.track)
            .put("strength", clip.strength.toDouble()))
        j.put("effectClips", clips)

        // Android AtomicFile conserva una copia previa si se corta la alimentación
        // o el proceso mientras se escriben los ajustes del proyecto.
        val file = AtomicFile(File(meta.dir, JSON))
        val stream = file.startWrite()
        try {
            stream.write(j.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Throwable) {
            file.failWrite(stream)
            throw error
        }
    }

    fun delete(id: String) {
        File(root, id).deleteRecursively()
    }

    private fun r4(v: Float): Double = (v * 10000f).roundToInt() / 10000.0

    private fun JSONArray.f(i: Int): Float = getDouble(i).toFloat()

    companion object {
        const val JSON = "project.json"
        const val THUMB = "thumb.jpg"
        const val EDIT_PREVIEW = "edit_preview.jpg"
        const val IMAGE = "source.img"
    }
}
