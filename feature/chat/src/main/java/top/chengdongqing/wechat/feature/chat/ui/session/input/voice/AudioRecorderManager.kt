package top.chengdongqing.wechat.feature.chat.ui.session.input.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.network.audio.ChatOpusFileWriter
import top.chengdongqing.wechat.core.network.audio.IntercomAudioFormat
import top.chengdongqing.wechat.core.network.audio.IntercomOpusEncoder
import java.io.File
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Records 16 kHz mono speech into the same Concentus Opus format used by intercom.
 */
class AudioRecorderManager @Inject constructor(
    private val privateFileManager: PrivateFileManager
) {
    private var audioRecord: AudioRecord? = null
    private var opusEncoder: IntercomOpusEncoder? = null
    private var opusWriter: ChatOpusFileWriter? = null
    @Volatile
    private var isRecording = false
    @Volatile
    private var currentAmplitude = 0f
    private var currentFile: File? = null
    private var recordingThread: Thread? = null

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = IntercomAudioFormat.SAMPLE_RATE
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
            IntercomAudioFormat.FRAME_BYTES * 4
        )
    }

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean = try {
        cleanup()
        currentFile = File.createTempFile("REC_", ".wopus")
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        ).apply {
            check(state == AudioRecord.STATE_INITIALIZED) { "AudioRecord init failed" }
        }
        opusEncoder = IntercomOpusEncoder(enableFec = false)
        opusWriter = ChatOpusFileWriter(checkNotNull(currentFile))
        audioRecord?.startRecording()
        isRecording = true
        startRecordingThread()
        true
    } catch (error: Exception) {
        Log.e(TAG, "启动 Opus 录音失败", error)
        cleanup()
        false
    }

    private fun startRecordingThread() {
        recordingThread = Thread({
            val frame = ShortArray(IntercomAudioFormat.FRAME_SAMPLES)
            var offset = 0
            while (isRecording) {
                val count = audioRecord?.read(frame, offset, frame.size - offset) ?: break
                if (count <= 0) continue
                calculateAmplitude(frame, offset, count)
                offset += count
                if (offset == frame.size) {
                    val packet = opusEncoder?.encode(frame) ?: break
                    opusWriter?.write(packet)
                    offset = 0
                }
            }
        }, "OpusRecordingThread").apply { start() }
    }

    private fun calculateAmplitude(buffer: ShortArray, offset: Int, count: Int) {
        var sum = 0.0
        for (index in offset until offset + count) {
            val value = buffer[index].toDouble()
            sum += value * value
        }
        val rms = sqrt(sum / count.coerceAtLeast(1))
        val db = 20 * log10(rms.coerceAtLeast(1.0) / 32767.0)
        val normalized = sqrt(((db + 50) / 50).coerceIn(0.0, 1.0)).toFloat()
        currentAmplitude = currentAmplitude * 0.8f + normalized * 0.2f
    }

    suspend fun stopRecording(): String? {
        if (!isRecording) return null
        return withContext(Dispatchers.IO) {
            try {
                stopCapture()
                val file = currentFile
                if (file != null && file.exists() && file.length() > 32) {
                    privateFileManager.saveMedia(MessageType.Voice, file).getOrNull()
                } else null
            } catch (error: Exception) {
                Log.e(TAG, "停止 Opus 录音失败", error)
                null
            } finally {
                currentFile?.delete()
                currentFile = null
            }
        }
    }

    fun cancelRecording() {
        stopCapture()
        currentFile?.delete()
        currentFile = null
    }

    fun getAmplitude(): Float = currentAmplitude

    private fun stopCapture() {
        isRecording = false
        runCatching { audioRecord?.stop() }
        recordingThread?.join(1_000)
        runCatching { audioRecord?.release() }
        audioRecord = null
        recordingThread = null
        runCatching { opusWriter?.close() }
        opusWriter = null
        opusEncoder = null
    }

    private fun cleanup() {
        stopCapture()
        currentAmplitude = 0f
    }
}
