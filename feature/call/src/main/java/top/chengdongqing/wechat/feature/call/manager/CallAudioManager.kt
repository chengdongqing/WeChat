package top.chengdongqing.wechat.feature.call.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import top.chengdongqing.wechat.core.common.media.RingtoneSound
import top.chengdongqing.wechat.core.common.media.VibratorHelper
import top.chengdongqing.wechat.core.common.media.toUri
import top.chengdongqing.wechat.core.data.repository.NotificationSettingsRepository
import top.chengdongqing.wechat.core.designsystem.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话音频管理器
 *
 * 职责：
 * - 音频路由切换（听筒 ↔ 免提），适配 Android 12+ 新 API
 * - 来电铃声播放与振动
 * - 通话接通振动反馈
 * - 通话结束提示音
 *
 * 状态管理：
 * enterCallMode 保存进入前的音频状态
 * exitCallMode 完整恢复原始状态
 */
@Singleton
class CallAudioManager @Inject constructor(
    private val vibratorHelper: VibratorHelper,
    private val notificationRepository: NotificationSettingsRepository,
    @param:ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var ringtonePlayer: MediaPlayer? = null

    /**
     * 保存通话前的音频设置
     */
    private var savedAudioMode = AudioManager.MODE_NORMAL
    private var savedSpeakerState = false

    /**
     * 进入通话音频模式
     *
     * 保存当前音频模式和免提状态，切换为 MODE_IN_COMMUNICATION
     *
     * @param isSpeakerOn 是否开启免提
     */
    fun enterCallMode(isSpeakerOn: Boolean) {
        savedAudioMode = audioManager.mode
        savedSpeakerState = isSpeakerOn()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeakerphoneOn(isSpeakerOn)
    }

    /**
     * 退出通话音频模式
     *
     * 恢复进入前的音频状态，停止铃声，清除通信设备路由
     */
    fun exitCallMode() {
        stopRingtone()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }

        audioManager.mode = savedAudioMode
        setSpeakerphoneOn(savedSpeakerState)
    }

    /**
     * 切换免提状态
     *
     * @return 切换后的状态（true = 免提已开启）
     */
    fun toggleSpeaker(): Boolean {
        val newState = !isSpeakerOn()
        setSpeakerphoneOn(newState)
        return newState
    }

    /**
     * 查询当前免提状态
     *
     * Android 12+ 通过通信设备类型判断
     * Android 12- 通过 isSpeakerphoneOn 判断
     *
     * @return true-免提开启，false-免提关闭
     */
    fun isSpeakerOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
    }

    /**
     * 设置免提路由
     *
     * Android 12+：
     * - 开启时查找 BUILTIN_SPEAKER 设备并设为通信设备
     * - 关闭时 clearCommunicationDevice，恢复系统默认路由（听筒或蓝牙）
     *
     * Android 12-：
     * - 直接设置 isSpeakerphoneOn
     *
     * @param on true-开启免提，false-关闭免提
     */
    fun setSpeakerphoneOn(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val deviceType = if (on) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }

            val device = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == deviceType }

            if (device != null) {
                audioManager.setCommunicationDevice(device)
            } else {
                audioManager.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = on
        }
    }

    /**
     * 播放铃声并振动
     *
     * 铃声循环播放直到调用 stopRingtone
     * 来电时额外触发振动（静音模式下跳过）
     */
    suspend fun startRingtone(isIncoming: Boolean, ringtone: RingtoneSound? = null) {
        stopRingtone()

        val ringtoneUri = (ringtone ?: myRingtone()).toUri(context)
        ringtonePlayer = MediaPlayer.create(context, ringtoneUri)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            start()
        }

        if (isIncoming) {
            vibratorHelper.vibrate(longArrayOf(0, 1000, 1000), repeat = 0)
        }
    }

    /**
     * 停止铃声和振动
     */
    fun stopRingtone() {
        ringtonePlayer?.run {
            if (isPlaying) stop()
            release()
        }
        ringtonePlayer = null
        vibratorHelper.cancel()
    }

    /**
     * 播放通话结束提示音
     *
     * 播放完成后自动释放资源
     *
     * @param onComplete 播放完成回调
     */
    fun playHangupTone(onComplete: () -> Unit) {
        MediaPlayer.create(context, R.raw.tip_call_end)?.apply {
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

    /**
     * 接通时的双击振动反馈
     */
    fun vibrateOnConnected() {
        vibratorHelper.vibrate(longArrayOf(0, 200, 300, 200))
    }

    private suspend fun myRingtone(): RingtoneSound =
        notificationRepository.ringtone.first()
}