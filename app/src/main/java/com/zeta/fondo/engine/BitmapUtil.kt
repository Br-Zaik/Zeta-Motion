package com.zeta.fondo.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapUtil {

    fun bounds(file: File): Pair<Int, Int> {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, o)
        return o.outWidth to o.outHeight
    }

    /**
     * Vista previa nítida con menos trabajo de CPU. En Android 9+ ImageDecoder
     * decodifica directamente al tamaño objetivo, evitando cargar primero una imagen
     * enorme para luego reducirla. En Android 8 usa el camino clásico.
     */
    fun decodePreview(file: File, maxSide: Int): Bitmap {
        if (Build.VERSION.SDK_INT < 28) return decodeAtMost(file, maxSide)
        val source = ImageDecoder.createSource(file)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val longSide = max(w, h)
            if (longSide > maxSide) {
                val k = maxSide.toFloat() / longSide.toFloat()
                decoder.setTargetSize(max(1, (w * k).roundToInt()), max(1, (h * k).roundToInt()))
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    }

    /**
     * Decodifica la imagen con su lado largo como máximo [maxSide].
     * Primero reduce con inSampleSize (rápido y sin gastar memoria) y luego
     * ajusta con filtrado para que el tamaño final sea exacto.
     */
    fun decodeAtMost(file: File, maxSide: Int): Bitmap {
        val (w, h) = bounds(file)
        if (w <= 0 || h <= 0) throw IOException("No se pudo leer la imagen")
        val long = max(w, h)
        var sample = 1
        while (long / (sample * 2) >= maxSide) sample *= 2
        val o = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, o)
            ?: throw IOException("No se pudo decodificar la imagen")
        val dl = max(decoded.width, decoded.height)
        if (dl <= maxSide) return decoded
        val k = maxSide.toFloat() / dl
        val nw = max(1, (decoded.width * k).roundToInt())
        val nh = max(1, (decoded.height * k).roundToInt())
        val scaled = Bitmap.createScaledBitmap(decoded, nw, nh, true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }
}
