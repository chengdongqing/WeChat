package top.chengdongqing.wechat.features.chat.ui.session.input.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * 音频焦点管理器：用于协调多个应用间的音频播放冲突。
 * 核心逻辑：
 * 1. 在录音或播放开始前申请焦点，通知其他应用（如网易云音乐、抖音）暂停或降低音量。
 * 2. 在录音或播放结束后释放焦点，允许其他应用恢复播放。
 */
class AudioFocusManager(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Android 8.0 (API 26) 及以上版本所需的焦点请求配置对象
     */
    private var focusRequest: AudioFocusRequest? = null

    /**
     * 申请瞬时音频焦点 (Transient Focus)
     * * @return 是否成功获取焦点。若返回 true，则其他后台音频已按系统指令暂停或静音。
     */
    fun requestFocus(): Boolean {
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION) // 提示音/辅助类用途
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)     // 内容类型为语音
                    .build()
            )
            // 是否接受延迟获取焦点（例如在通话中时，是否排队等待）
            .setAcceptsDelayedFocusGain(false)
            // 监听焦点变化回调（如被电话顶掉时的逻辑）
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    // 永久失去焦点
                    AudioManager.AUDIOFOCUS_LOSS -> {}
                    // 临时失去焦点
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {}
                }
            }
            .build()

        return audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 释放音频焦点
     * 务必在录音结束、取消或发生异常时调用，否则会导致其他应用（如音乐播放器）无法自动恢复。
     */
    fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }
}