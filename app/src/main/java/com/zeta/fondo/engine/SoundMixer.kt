package com.zeta.fondo.engine

import com.zeta.fondo.model.ExportSettings
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.tanh

/**
 * Mezcla la música elegida (fragmento del bucle, con fundido opcional) y los sonidos de ambiente.
 * Devuelve PCM 16 bits intercalado. La vista previa y el MP4 usan exactamente esta mezcla.
 */
class SoundMixer(musicFile: File?, private var s: ExportSettings, private var boltCount: Int) : AutoCloseable {

    private val music: PcmReader? = if (musicOn(s, musicFile)) PcmReader(musicFile!!, s) else null
    val rate: Int = if (music != null) s.audioRate else 44100
    val channels: Int = if (music != null) s.audioChannels else 2
    val loopFrames: Long get() = s.loopSec.toLong() * rate

    fun update(settings: ExportSettings, bolt: Int) {
        s = settings
        boltCount = bolt
        music?.update(settings)
    }

    fun read(startFrame: Long, count: Int): ByteArray {
        val out = ByteArray(count * channels * 2)
        val loop = loopFrames.coerceAtLeast(1L)
        val musicBytes = if (music != null && s.audioEnabled && s.soundVolume > 0f) music.read(startFrame, count) else null
        val mb = musicBytes?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN) }
        val amb = Ambient.mix(s.ambient, rate, loop.toInt(), boltCount)
        val ob = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        val fade = (rate * 0.25f).coerceAtLeast(1f)
        for (i in 0 until count) {
            val pos = ((startFrame + i) % loop + loop) % loop
            val gain = if (s.audioFade && mb != null) min(1f, min(pos / fade, (loop - pos) / fade)) else 1f
            var ambL = 0f
            var ambR = 0f
            val p = (pos * 2).toInt()
            for ((data, vol) in amb) {
                if (p + 1 < data.size) {
                    ambL += data[p] / 32768f * vol
                    ambR += data[p + 1] / 32768f * vol
                }
            }
            for (c in 0 until channels) {
                var v = if (channels == 2) (if (c == 0) ambL else ambR) else (ambL + ambR) * 0.5f
                if (mb != null) v += mb.getShort((i * channels + c) * 2) / 32768f * gain
                ob.putShort((i * channels + c) * 2, (softClip(v) * 32767f).toInt().toShort())
            }
        }
        return out
    }

    private fun softClip(v: Float): Float {
        val a = abs(v)
        if (a < 0.8f) return v
        val shaped = 0.8f + 0.2f * tanh((a - 0.8f) / 0.2f)
        return if (v < 0f) -shaped else shaped
    }

    override fun close() {
        music?.close()
    }

    companion object {
        fun musicOn(s: ExportSettings, file: File?): Boolean =
            s.audioName.isNotBlank() && s.audioEnabled && s.soundVolume > 0f && s.audioFrames > 0L && file?.isFile == true

        fun hasSound(s: ExportSettings, file: File?): Boolean = musicOn(s, file) || s.hasAmbient
    }
}
