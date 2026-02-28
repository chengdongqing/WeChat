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
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chengdongqing.wechat.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话音频管理器
 *
 * 职责：
 * - 音频路由切换（听筒 ↔ 免提），适配 Android 12+ 新 API
 * - 来电铃声播放与震动
 * - 通话接通震动反馈
 * - 通话结束提示音
 *
 * 状态：[enterCallMode] 保存进入前的音频状态，[exitCallMode] 完整恢复。
 */
@Singleton
class CallAudioManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var ringtonePlayer: MediaPlayer? = null
    private val vibrator: Vibrator? = resolveVibrator()

    private var savedAudioMode = AudioManager.MODE_NORMAL
    private var savedSpeakerState = false

    // ==================== 通话音频模式 ====================

    /**
     * 进入通话音频模式
     *
     * 保存当前音频模式和免提状态，切换为 MODE_IN_COMMUNICATION。
     */
    fun enterCallMode(isSpeakerOn: Boolean) {
        savedAudioMode = audioManager.mode
        savedSpeakerState = isSpeakerOn()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeakerphoneOn(isSpeakerOn)
    }

    /**
     * 退出通话音频模式，恢复进入前的状态
     *
     * 同时停止铃声，清除通信设备路由（Android 12+）。
     */
    fun exitCallMode() {
        stopRingtone()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        audioManager.mode = savedAudioMode
        setSpeakerphoneOn(savedSpeakerState)
    }

    // ==================== 免提切换 ====================

    /** 切换免提状态，返回切换后的状态（true = 免提已开） */
    fun toggleSpeaker(): Boolean {
        val newState = !isSpeakerOn()
        setSpeakerphoneOn(newState)
        return newState
    }

    /** 查询当前免提状态，Android 12+ 通过通信设备类型判断 */
    fun isSpeakerOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
    }

    /**
     * 设置免提路由，适配 Android 12+
     *
     * Android 12+：开启时查找 BUILTIN_SPEAKER 设备并设为通信设备；
     *              关闭时 clearCommunicationDevice，恢复系统默认路由（听筒或蓝牙）。
     * Android 12-：直接设置 isSpeakerphoneOn。
     */
    fun setSpeakerphoneOn(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (on) {
                audioManager.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    ?.let { audioManager.setCommunicationDevice(it) }
            } else {
                audioManager.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = on
        }
    }

    // ==================== 铃声 ====================

    /**
     * 播放铃声并震动
     *
     * 铃声循环播放直到 [stopRingtone]。
     * 来电时额外触发震动（静音模式下跳过）。
     */
    fun startRingtone(isIncoming: Boolean) {
        if (ringtonePlayer?.isPlaying == true) return

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

        if (isIncoming && shouldVibrate()) {
            vibrate(pattern = longArrayOf(0, 1000, 1000), repeat = 0)
        }
    }

    /** 停止铃声和震动 */
    fun stopRingtone() {
        ringtonePlayer?.run {
            if (isPlaying) stop()
            release()
        }
        ringtonePlayer = null
        vibrator?.cancel()
    }

    /** 播放通话结束提示音，播完自动释放 */
    fun playHangupTone(onComplete: () -> Unit) {
        MediaPlayer.create(context, R.raw.playend)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            )
            setOnCompletionListener {
                it.release()
                onComplete()
            }
            start()
        }
    }

    // ==================== 震动 ====================

    /** 接通时的双击震动反馈 */
    fun vibrateOnConnected() {
        vibrate(pattern = longArrayOf(0, 200, 300, 200), repeat = -1)
    }

    /** 非静音模式下才震动 */
    private fun shouldVibrate() =
        audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT

    private fun vibrate(pattern: LongArray, repeat: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, repeat))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, repeat)
        }
    }

    /** 获取 Vibrator，适配 Android 12+ VibratorManager */
    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}