package top.chengdongqing.wechat.features.call.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chengdongqing.wechat.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话音频管理器
 *
 * 职责:
 * 1. 音频路由切换（听筒 ↔ 免提）
 * 2. 来电铃声 + 震动
 * 3. 拨号等待音
 * 4. 通话结束提示音
 */
@Singleton
class CallAudioManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "CallAudioManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var ringtonePlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = getVibrator()

    private var savedAudioMode: Int = AudioManager.MODE_NORMAL
    private var savedSpeakerState: Boolean = false

    // ==================== 通话音频模式 ====================

    /**
     * 进入通话模式
     *
     * 保存当前音频状态，切换到通话模式。
     */
    fun enterCallMode(isSpeakerOn: Boolean) {
        savedAudioMode = audioManager.mode
        savedSpeakerState = isSpeakerOn()

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeakerphoneOn(isSpeakerOn)
        Log.d(TAG, "进入通话音频模式")
    }

    /**
     * 退出通话模式，恢复之前的音频状态
     */
    fun exitCallMode() {
        stopRingtone()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }

        audioManager.mode = savedAudioMode
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = savedSpeakerState
        Log.d(TAG, "退出通话音频模式")
    }

    // ==================== 免提切换 ====================

    fun toggleSpeaker(): Boolean {
        val currentState = isSpeakerOn()
        val newState = !currentState
        setSpeakerphoneOn(newState)
        Log.d(TAG, "免提: $newState")
        return newState
    }

    fun isSpeakerOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.communicationDevice
            device?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
    }

    /**
     * 设置免提状态（适配 Android 12+）
     */
    fun setSpeakerphoneOn(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (on) {
                // 开启免提：寻找类型为 BUILTIN_SPEAKER 的设备
                val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                speakerDevice?.let { audioManager.setCommunicationDevice(it) }
            } else {
                // 关闭免提：清除设置，恢复系统默认路由（通常是听筒或蓝牙）
                audioManager.clearCommunicationDevice()
            }
        } else {
            // 旧版本适配
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = on
        }
    }

    // ==================== 铃声 ====================

    /**
     * 播放铃声 + 震动
     */
    fun startRingtone(isIncoming: Boolean) {
        if (ringtonePlayer?.isPlaying == true) return

        // 铃声
        ringtonePlayer = MediaPlayer.create(context, R.raw.phonering)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            start()
        }

        // 震动
        if (isIncoming && shouldVibrate()) {
            val pattern = longArrayOf(0, 1000, 1000)
            vibrate(pattern, 0)
        }

        Log.d(TAG, "来电铃声已播放")
    }

    fun stopRingtone() {
        ringtonePlayer?.run {
            if (isPlaying) stop()
            release()
        }
        ringtonePlayer = null

        vibrator?.cancel()
    }

    /**
     * 通话结束音
     */
    fun playHangupTone() {
        MediaPlayer.create(context, R.raw.playend)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            )
            setOnCompletionListener { it.release() }
            start()
        }
    }

    // ==================== 工具 ====================

    /**
     * 接通时的震动反馈
     */
    fun vibrateOnConnected() {
        val pattern = longArrayOf(0, 200, 300, 200)
        vibrate(pattern, -1)
    }

    /**
     * 检查系统是否静音模式
     */
    private fun shouldVibrate(): Boolean {
        val ringerMode = audioManager.ringerMode
        return ringerMode != AudioManager.RINGER_MODE_SILENT
    }

    private fun vibrate(pattern: LongArray, repeat: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, repeat))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, repeat)
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}