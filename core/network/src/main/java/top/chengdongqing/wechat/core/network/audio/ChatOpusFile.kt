package top.chengdongqing.wechat.core.network.audio

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile

/** Small seekable container for chat Opus packets. Integers are big-endian. */
class ChatOpusFileWriter(private val file: File) : AutoCloseable {
    private val output = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))
    private var totalSamples = 0L

    init {
        output.write(MAGIC)
        output.writeInt(IntercomAudioFormat.SAMPLE_RATE)
        output.writeInt(IntercomAudioFormat.FRAME_SAMPLES)
        output.writeLong(0L)
    }

    fun write(packet: ByteArray, samples: Int = IntercomAudioFormat.FRAME_SAMPLES) {
        require(packet.size <= MAX_PACKET_BYTES)
        output.writeInt(packet.size)
        output.write(packet)
        totalSamples += samples
    }

    override fun close() {
        output.close()
        RandomAccessFile(file, "rw").use {
            it.seek(TOTAL_SAMPLES_OFFSET)
            it.writeLong(totalSamples)
        }
    }

    companion object {
        private val MAGIC = "WCOPUS01".encodeToByteArray()
        private const val TOTAL_SAMPLES_OFFSET = 16L
        private const val MAX_PACKET_BYTES = 4_096

        fun isChatOpus(file: File): Boolean = runCatching {
            FileInputStream(file).use { input ->
                input.readNBytes(MAGIC.size).contentEquals(MAGIC)
            }
        }.getOrDefault(false)

        /** Decode through the same Concentus decoder used by intercom into a standard PCM WAV. */
        fun decodeToWav(source: File, target: File) {
            DataInputStream(BufferedInputStream(FileInputStream(source))).use { input ->
                require(
                    input.readNBytes(MAGIC.size).contentEquals(MAGIC)
                ) { "Invalid chat Opus file" }
                val sampleRate = input.readInt()
                val frameSamples = input.readInt()
                val totalSamples = input.readLong()
                require(sampleRate == IntercomAudioFormat.SAMPLE_RATE)
                require(frameSamples == IntercomAudioFormat.FRAME_SAMPLES)

                val pcmBytes = totalSamples.coerceAtMost(Int.MAX_VALUE.toLong() / 2).toInt() * 2
                DataOutputStream(BufferedOutputStream(FileOutputStream(target))).use { output ->
                    writeWavHeader(output, sampleRate, pcmBytes)
                    val decoder = IntercomOpusDecoder()
                    while (input.available() > 0) {
                        val size = input.readInt()
                        require(size in 1..MAX_PACKET_BYTES)
                        val packet = input.readNBytes(size)
                        require(packet.size == size)
                        decoder.decode(packet).forEach { sample ->
                            output.writeByte(sample.toInt() and 0xFF)
                            output.writeByte((sample.toInt() ushr 8) and 0xFF)
                        }
                    }
                }
            }
        }

        private fun writeWavHeader(output: DataOutputStream, sampleRate: Int, pcmBytes: Int) {
            fun ascii(value: String) = output.write(value.encodeToByteArray())
            fun le16(value: Int) {
                output.writeByte(value and 0xFF)
                output.writeByte(value ushr 8 and 0xFF)
            }

            fun le32(value: Int) {
                output.writeByte(value and 0xFF)
                output.writeByte(value ushr 8 and 0xFF)
                output.writeByte(value ushr 16 and 0xFF)
                output.writeByte(value ushr 24 and 0xFF)
            }
            ascii("RIFF"); le32(36 + pcmBytes); ascii("WAVEfmt "); le32(16)
            le16(1); le16(1); le32(sampleRate); le32(sampleRate * 2); le16(2); le16(16)
            ascii("data"); le32(pcmBytes)
        }
    }
}
