package com.zeta.fondo.engine

import android.content.Context
import android.net.Uri
import android.media.*
import android.provider.OpenableColumns
import com.zeta.fondo.model.ExportSettings
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/** Imported recordings decoded once; preview and export share the same PCM source. */
object ImportedAudio {
    const val FILE_NAME = "audio.pcm"
    data class Decoded(val name: String, val rate: Int, val channels: Int, val frames: Long)

    fun decode(context: Context, uri: Uri, target: File): Decoded {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "Audio importado"
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val index = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("El archivo no contiene audio")
            extractor.selectTrack(index)
            val format = extractor.getTrackFormat(index)
            if (format.containsKey(MediaFormat.KEY_DURATION)) require(format.getLong(MediaFormat.KEY_DURATION) <= 600_000_000L) {
                "Elige una grabación de hasta 10 minutos"
            }
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            var inputEnd = false
            var done = false
            var bytes = 0L
            var idle = 0
            val info = MediaCodec.BufferInfo()
            target.outputStream().buffered().use { out ->
                while (!done) {
                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
                    var progress = false
                    if (!inputEnd) {
                        val i = decoder.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val buffer = decoder.getInputBuffer(i)!!
                            buffer.clear()
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnd = true
                            } else {
                                decoder.queueInputBuffer(i, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                            progress = true
                        }
                    }
                    val i = decoder.dequeueOutputBuffer(info, 10_000)
                    if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val f = decoder.outputFormat
                        rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        encoding = if (f.containsKey(MediaFormat.KEY_PCM_ENCODING)) f.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                        require(channels in 1..2) { "Usa audio mono o estéreo" }
                        require(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT) { "Formato PCM no compatible" }
                        progress = true
                    } else if (i >= 0) {
                        val b = decoder.getOutputBuffer(i)!!
                        b.position(info.offset); b.limit(info.offset + info.size); b.order(ByteOrder.LITTLE_ENDIAN)
                        val data = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                            val dst = java.nio.ByteBuffer.allocate(info.size / 2).order(ByteOrder.LITTLE_ENDIAN)
                            while (b.remaining() >= 4) dst.putShort((b.float.coerceIn(-1f, 1f) * 32767).toInt().toShort())
                            dst.array()
                        } else ByteArray(info.size).also { b.get(it) }
                        out.write(data)
                        bytes += data.size
                        require(bytes <= rate.toLong() * channels * 2 * 600 && bytes <= 128L * 1024 * 1024) { "La grabación es demasiado larga" }
                        done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(i, false)
                        progress = true
                    }
                    idle = if (progress) 0 else idle + 1
                    check(idle < 500) { "El decodificador no responde" }
                }
            }
            require(channels in 1..2 && rate in 8000..96000 && bytes > 0) { "Audio vacío o incompatible" }
            return Decoded(name, rate, channels, bytes / (2 * channels))
        } catch (e: Exception) {
            target.delete()
            throw e
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            codec?.release()
            extractor.release()
        }
    }
    data class Packet(val bytes: ByteArray, val info: MediaCodec.BufferInfo)
    data class Encoded(val format: MediaFormat, val packets: List<Packet>)

    fun encode(file: java.io.File, s: ExportSettings, cancel: AtomicBoolean, boltCount: Int = 0): Encoded {
        val reader = SoundMixer(file, s, boltCount)
        val RATE = reader.rate
        val CHANNELS = reader.channels
        val duration = s.loopSec.toLong() * s.repeats
        val totalSamples = duration * RATE
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, RATE, CHANNELS).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 192000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val packets = ArrayList<Packet>()
        var outputFormat: MediaFormat? = null
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var written = 0L
            var done = false
            var tries = 0
            while (!done) {
                if (cancel.get()) throw ExportCancelled()
                if (written <= totalSamples) {
                    val index = codec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val count = minOf(1024L, totalSamples - written).toInt()
                        val buffer = codec.getInputBuffer(index) ?: error("Codificador AAC sin búfer de entrada")
                        buffer.clear()
                        buffer.order(ByteOrder.LITTLE_ENDIAN)
                        buffer.put(reader.read(written, count))
                        codec.queueInputBuffer(index, 0, count * 2 * CHANNELS, written * 1_000_000L / RATE,
                            if (count == 0) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        written += if (count == 0) 1 else count
                    }
                }
                while (true) {
                    val index = codec.dequeueOutputBuffer(info, 10_000)
                    if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (++tries > 500) error("AAC no terminó la codificación")
                        break
                    }
                    tries = 0
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) outputFormat = codec.outputFormat
                    else if (index >= 0) {
                        if (info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            val src = codec.getOutputBuffer(index) ?: error("AAC sin búfer de salida")
                            src.position(info.offset)
                            src.limit(info.offset + info.size)
                            val bytes = ByteArray(info.size)
                            src.get(bytes)
                            val copy = MediaCodec.BufferInfo().apply {
                                set(0, bytes.size, info.presentationTimeUs,
                                    info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv())
                            }
                            packets.add(Packet(bytes, copy))
                        }
                        done = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        codec.releaseOutputBuffer(index, false)
                        if (done) break
                    }
                }
            }
            return Encoded(outputFormat ?: error("AAC no entregó formato de salida"), packets)
        } finally {
            reader.close()
            try { codec.stop() } catch (_: Exception) { }
            codec.release()
        }
    }
}
