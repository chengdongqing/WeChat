package top.chengdongqing.wechat.ui.chat.session.input.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceRecordManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    /**
     * 开始录音
     * @return 录音文件路径
     */
    fun startRecording(): String? {
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        currentFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // 容器格式
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)   // 微信通用编码
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("VoiceRecordManager", "Prepare/Start failed", e)
                return null
            }
        }
        return file.absolutePath
    }

    /**
     * 停止录音
     */
    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecordManager", "Stop failed", e)
        }
        mediaRecorder = null
    }

    /**
     * 获取当前振幅 (0.0 到 1.0)
     */
    fun getAmplitude(): Float {
        return try {
            // maxAmplitude 范围是 0..32767
            val max = mediaRecorder?.maxAmplitude ?: 0
            (max / 32767f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * 放弃录音并删除文件
     */
    fun cancelRecording() {
        stopRecording()
        currentFile?.delete()
        currentFile = null
    }
}