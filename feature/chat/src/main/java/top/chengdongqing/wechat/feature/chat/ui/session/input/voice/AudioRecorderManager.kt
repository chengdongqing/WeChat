package top.chengdongqing.wechat.feature.chat.ui.session.input.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.MessageType
import java.io.File
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 音频录制管理类
 * 采用 AudioRecord 采集 + MediaCodec AAC 编码 + MediaMuxer 封装 M4A
 */
class AudioRecorderManager @Inject constructor(
    private val privateFileManager: PrivateFileManager
) {

    private var audioRecord: AudioRecord? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null

    private var trackIndex = -1
    private var muxerStarted = false
    private var encodedPcmFrames = 0L

    @Volatile
    private var isRecording = false

    @Volatile
    private var currentAmplitude = 0f

    private var currentFile: File? = null
    private var recordingThread: Thread? = null

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val BIT_RATE = 96000
        private val BUFFER_SIZE =
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }

    /**
     * 开始录音
     * @return 是否启动成功
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        return try {
            cleanup()

            // 文件准备
            currentFile = File.createTempFile("REC_", ".m4a")

            // 初始化 AudioRecord (硬件降噪模式)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE
            ).apply {
                if (state != AudioRecord.STATE_INITIALIZED) throw IllegalStateException("AudioRecord init failed")
            }

            // 配置 AAC 编码器
            val format = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1).apply {
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE)
            }
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            // 初始化封装器
            mediaMuxer =
                MediaMuxer(currentFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // 启动
            audioRecord?.startRecording()
            isRecording = true
            muxerStarted = false
            trackIndex = -1
            encodedPcmFrames = 0L

            startRecordingThread()
            Log.i(TAG, "开始录音: ${currentFile?.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败: ${e.message}")
            cleanup()
            false
        }
    }

    private fun startRecordingThread() {
        recordingThread = Thread({
            val buffer = ShortArray(BUFFER_SIZE / 2)
            val bufferInfo = MediaCodec.BufferInfo()

            while (isRecording) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    calculateAmplitude(buffer, readSize)
                    encodePCM(buffer, readSize, bufferInfo)
                }
            }
        }, "AudioRecordingThread").apply { start() }
    }

    private fun calculateAmplitude(buffer: ShortArray, size: Int) {
        var sum = 0.0
        for (i in 0 until size) sum += (buffer[i] * buffer[i]).toDouble()
        val rms = sqrt(sum / size)

        // 转为分贝 (dB)
        val db = 20 * log10(rms.coerceAtLeast(1.0) / 32767.0)
        // 映射到 0..1 (假设底噪是 -50dB)
        var normalized = ((db + 50) / 50).coerceIn(0.0, 1.0)
        // 使用 sqrt 曲线：让微弱的波动在视觉上更“活跃”
        normalized = sqrt(normalized)

        // 一阶低通滤波：让音量条“丝滑”摆动，而不是闪烁跳动
        // 这里的 0.2f 是平滑系数，越小越丝滑，越大越跟手
        currentAmplitude = currentAmplitude * 0.8f + normalized.toFloat() * 0.2f
    }

    private fun encodePCM(data: ShortArray, size: Int, bufferInfo: MediaCodec.BufferInfo) {
        val codec = mediaCodec ?: return

        // 入队 PCM 数据
        runCatching {
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val presentationTimeUs = encodedPcmFrames * 1_000_000L / SAMPLE_RATE
                codec.getInputBuffer(inputIndex)?.apply {
                    clear()
                    asShortBuffer().put(data, 0, size) // 批量操作性能更优
                    codec.queueInputBuffer(inputIndex, 0, size * 2, presentationTimeUs, 0)
                }
                encodedPcmFrames += size
            }
        }

        // 出队编码数据
        processOutputBuffer(bufferInfo)
    }

    private fun processOutputBuffer(bufferInfo: MediaCodec.BufferInfo, timeoutUs: Long = 0) {
        val codec = mediaCodec ?: return
        var outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)

        while (outputIndex >= 0 || outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!muxerStarted) {
                    trackIndex = mediaMuxer?.addTrack(codec.outputFormat) ?: -1
                    mediaMuxer?.start()
                    muxerStarted = true
                    Log.d(TAG, "封装器格式确定并启动")
                }
            } else {
                if (muxerStarted && trackIndex >= 0 && bufferInfo.size > 0) {
                    codec.getOutputBuffer(outputIndex)?.let { buffer ->
                        buffer.position(bufferInfo.offset)
                        buffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer?.writeSampleData(trackIndex, buffer, bufferInfo)
                    }
                }
                codec.releaseOutputBuffer(outputIndex, false)
            }
            outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    /**
     * 停止录音并保存
     */
    suspend fun stopRecording(): String? {
        if (!isRecording) return null

        return withContext(Dispatchers.IO) {
            try {
                // 发送结束信号给编码线程
                isRecording = false
                // 等待读取线程结束（确保所有 PCM 都塞进了 Codec）
                recordingThread?.join(1000)
                // 彻底排空编码器并关闭封装器
                drainEncoderAndStopMuxer()

                val file = currentFile
                if (file != null && file.exists() && file.length() > 500) {
                    val result = privateFileManager.saveMedia(
                        messageType = MessageType.Voice,
                        sourceFile = file
                    ).getOrNull()

                    Log.i(TAG, "录音保存成功: $result, 大小: ${file.length()}")
                    result
                } else {
                    Log.e(TAG, "录音文件无效或太小: ${file?.length()} bytes")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "停止录音失败", e)
                null
            } finally {
                cleanup()
                currentFile?.delete()
            }
        }
    }

    private fun drainEncoderAndStopMuxer() {
        runCatching {
            drainEncoder()
            if (muxerStarted) {
                mediaMuxer?.stop()
                muxerStarted = false
            }
        }
    }

    /**
     * 排空编码器残留数据
     */
    private fun drainEncoder() {
        val codec = mediaCodec ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        runCatching {
            // 发送 EOS 信号
            val inputIndex = codec.dequeueInputBuffer(1000)
            if (inputIndex >= 0) {
                val presentationTimeUs = encodedPcmFrames * 1_000_000L / SAMPLE_RATE
                codec.queueInputBuffer(
                    inputIndex,
                    0,
                    0,
                    presentationTimeUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            // 循环拉取直到收到 EOS 标记
            var eosReceived = false
            var retry = 0
            while (!eosReceived && retry < 50) {
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 1000)
                if (outIndex >= 0) {
                    if (muxerStarted && bufferInfo.size > 0) {
                        codec.getOutputBuffer(outIndex)?.let {
                            mediaMuxer?.writeSampleData(trackIndex, it, bufferInfo)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) eosReceived =
                        true
                } else {
                    retry++
                }
            }
        }
    }

    fun cancelRecording() {
        isRecording = false
        recordingThread?.join(500)
        currentFile?.delete()
        cleanup()
        Log.i(TAG, "录音已取消并删除文件")
    }

    fun getAmplitude(): Float = currentAmplitude

    private fun cleanup() {
        isRecording = false
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        runCatching { mediaCodec?.stop() }
        runCatching { mediaCodec?.release() }
        runCatching { if (muxerStarted) mediaMuxer?.stop() }
        runCatching { mediaMuxer?.release() }

        audioRecord = null
        mediaCodec = null
        mediaMuxer = null
        recordingThread = null
        trackIndex = -1
        muxerStarted = false
        encodedPcmFrames = 0L
    }
}
