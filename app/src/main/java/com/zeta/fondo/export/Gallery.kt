package com.zeta.fondo.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Gallery {

    const val FOLDER = "FondoZeta"

    /** Copia el MP4 a Galería › Movies/FondoZeta. Devuelve la ruta legible. */
    fun saveVideo(ctx: Context, file: File, displayName: String): String {
        if (Build.VERSION.SDK_INT >= 29) {
            val cr = ctx.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + FOLDER)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("No se pudo crear el vídeo en la Galería")
            try {
                val out = cr.openOutputStream(uri) ?: throw IOException("No se pudo escribir en la Galería")
                out.use { o -> file.inputStream().use { it.copyTo(o) } }
                val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                cr.update(uri, done, null, null)
            } catch (e: Exception) {
                cr.delete(uri, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), FOLDER)
            dir.mkdirs()
            val dest = File(dir, displayName)
            file.inputStream().use { i -> FileOutputStream(dest).use { i.copyTo(it) } }
            MediaScannerConnection.scanFile(ctx, arrayOf(dest.absolutePath), arrayOf("video/mp4"), null)
        }
        return "Movies/$FOLDER/$displayName"
    }

    /** Guarda un cuadro PNG en Galería › Pictures/FondoZeta/<carpeta>/ */
    fun savePng(ctx: Context, bmp: Bitmap, subFolder: String, name: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            val cr = ctx.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + FOLDER + "/" + subFolder)
            }
            val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("No se pudo crear $name en la Galería")
            val out = cr.openOutputStream(uri) ?: throw IOException("No se pudo escribir $name")
            out.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "$FOLDER/$subFolder")
            dir.mkdirs()
            val f = File(dir, name)
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            MediaScannerConnection.scanFile(ctx, arrayOf(f.absolutePath), arrayOf("image/png"), null)
        }
    }
}

object ShareUtil {

    const val ALIGHT = "com.alightcreative.motion"

    private fun uriFor(ctx: Context, file: File): Uri =
        FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)

    private fun sendIntent(ctx: Context, file: File): Intent {
        val uri = uriFor(ctx, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Intenta enviar el vídeo directo a Alight Motion. Si no se puede, abre el menú Compartir. */
    fun sendToAlight(ctx: Context, file: File): Boolean {
        val direct = sendIntent(ctx, file).setPackage(ALIGHT)
        return try {
            ctx.startActivity(direct)
            true
        } catch (e: ActivityNotFoundException) {
            shareChooser(ctx, file)
            false
        } catch (e: SecurityException) {
            shareChooser(ctx, file)
            false
        }
    }

    fun shareChooser(ctx: Context, file: File) {
        val chooser = Intent.createChooser(sendIntent(ctx, file), "Enviar fondo animado")
        ctx.startActivity(chooser)
    }

    /** Abre el vídeo exportado en la Galería o el reproductor del teléfono. */
    fun openVideo(ctx: Context, file: File): Boolean {
        val uri = uriFor(ctx, file)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try { ctx.startActivity(i); true } catch (e: Exception) { false }
    }

    fun openAlight(ctx: Context): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(ALIGHT) ?: return false
        return try {
            ctx.startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }
}
