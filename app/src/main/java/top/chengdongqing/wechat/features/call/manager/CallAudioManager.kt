package top.chengdongqing.wechat.features.call.manager

import android.annotation.SuppressLint
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
    private var dialingPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private var savedAudioMode: Int = AudioManager.MODE_NORMAL
    private var savedSpeakerState: Boolean = false

    // ==================== 通话音频模式 ====================

    /**
     * 进入通话模式
     *
     * 保存当前音频状态，切换到通话模式。
     */
    fun enterCallMode() {
        savedAudioMode = audioManager.mode
        savedSpeakerState = isSpeakerOn()

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeakerphoneOn(false)   // 默认听筒
        Log.d(TAG, "进入通话音频模式")
    }

    /**
     * 退出通话模式，恢复之前的音频状态
     */
    fun exitCallMode() {
        stopRingtone()
        stopDialingTone()

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
     * 播放来电铃声 + 震动
     */
    @SuppressLint("MissingPermission")
    fun startRingtone() {
        // 铃声
        ringtonePlayer = MediaPlayer.create(context, R.raw.phonering)?.apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            start()
        }

        // 震动
        vibrator = getVibrator()?.apply {
            val pattern = longArrayOf(0, 500, 500)  // 震 500ms 停 500ms 循环
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrate(pattern, 0)
            }
        }

        Log.d(TAG, "来电铃声已播放")
    }

    @SuppressLint("MissingPermission")
    fun stopRingtone() {
        ringtonePlayer?.run {
            if (isPlaying) stop()
            release()
        }
        ringtonePlayer = null

        vibrator?.cancel()
        vibrator = null
    }

    // ==================== 拨号音 ====================

    fun startDialingTone() {
        dialingPlayer = MediaPlayer.create(context, R.raw.phonering)?.apply {
            isLooping = true
            start()
        }
    }

    fun stopDialingTone() {
        dialingPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        dialingPlayer = null
    }

    // ==================== 工具 ====================

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