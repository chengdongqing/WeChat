package top.chengdongqing.wechat.feature.discovery.moments

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import java.io.File

class VoiceOverRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    fun start() {
        stop(discard = true)
        output = File(context.cacheDir, "moment_voice_${System.currentTimeMillis()}.m4a")
        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(output!!.absolutePath)
            prepare()
            start()
        }
    }

    fun stop(discard: Boolean = false): Uri? {
        val active = recorder
        if (active == null) {
            val file = output ?: return null
            if (discard) {
                file.delete()
                output = null
                return null
            }
            return Uri.fromFile(file)
        }
        runCatching { active.stop() }
        active.release()
        recorder = null
        return output?.let { file ->
            if (discard) {
                file.delete()
                output = null
                null
            } else Uri.fromFile(file)
        }
    }
}
