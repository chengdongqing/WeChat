package top.chengdongqing.wechat.core.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build

/**
 * 语音播放器
 * 支持扬声器/听筒模式切换
 */
class VoicePlayer(context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * 保存播放前的音频设置，用于恢复
     */
    private var savedAudioMode = AudioManager.MODE_NORMAL
    private var savedSpeakerState = false

    val isPlaying: Boolean
        get() = isPrepared && runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false)

    val currentPosition: Int
        get() = if (isPrepared) {
            runCatching { mediaPlayer?.currentPosition ?: 0 }.getOrDefault(0)
        } else {
            0
        }

    val duration: Int
        get() = if (isPrepared) {
            runCatching { mediaPlayer?.duration ?: 0 }.getOrDefault(0)
        } else {
            0
        }

    /**
     * 播放语音
     *
     * @param localPath 本地文件路径
     * @param isSpeakerOn 是否使用扬声器模式（false为听筒模式）
     * @param onComplete 播放完成回调
     */
    fun play(
        localPath: String,
        isSpeakerOn: Boolean,
        speed: Float = 1f,
        onComplete: () -> Unit
    ) {
        if (mediaPlayer != null) {
            stop()
        }

        /**
         * 保存当前音频设置
         */
        savedAudioMode = audioManager.mode
        savedSpeakerState = isSpeakerOn()

        /**
         * 配置音频路由
         * 扬声器模式：MODE_NORMAL，使用外放
         * 听筒模式：MODE_IN_COMMUNICATION，使用听筒
         */
        setOutputMode(isSpeakerOn)

        isPrepared = false
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(localPath)

                /**
                 * 播放完成监听
                 */
                setOnCompletionListener {
                    isPrepared = false
                    onComplete()
                    stop()
                }

                /**
                 * 错误监听
                 */
                setOnErrorListener { _, _, _ ->
                    isPrepared = false
                    onComplete()
                    stop()
                    true
                }

                /**
                 * 设置音频属性
                 * 关键：根据播放模式设置不同的音频用途
                 */
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            if (isSpeakerOn) {
                                // 扬声器模式：媒体播放
                                AudioAttributes.USAGE_MEDIA
                            } else {
                                // 听筒模式：语音通话，这样音量会正常
                                AudioAttributes.USAGE_VOICE_COMMUNICATION
                            }
                        )
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )

                /**
                 * 异步准备并播放
                 */
                prepareAsync()
                setOnPreparedListener {
                    isPrepared = true
                    start()
                    if (speed != 1f) {
                        runCatching {
                            playbackParams = playbackParams.setSpeed(speed)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    fun pause() {
        if (isPrepared) {
            runCatching { if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause() }
        }
    }

    fun resume() {
        if (isPrepared) runCatching { mediaPlayer?.start() }
    }

    fun seekTo(positionMs: Int) {
        if (isPrepared) {
            runCatching { mediaPlayer?.seekTo(positionMs.coerceIn(0, duration)) }
        }
    }

    fun setSpeed(speed: Float) {
        if (isPrepared) runCatching {
            mediaPlayer?.playbackParams =
                (mediaPlayer?.playbackParams ?: PlaybackParams()).setSpeed(speed)
        }
    }

    /**
     * 在不打断当前播放进度的情况下切换扬声器/听筒。
     */
    fun setOutputMode(isSpeakerOn: Boolean) {
        audioManager.mode = if (isSpeakerOn) {
            AudioManager.MODE_NORMAL
        } else {
            AudioManager.MODE_IN_COMMUNICATION
        }
        setSpeakerphoneOn(isSpeakerOn)
    }

    /**
     * 设置扬声器开关
     */
    fun setSpeakerphoneOn(isSpeakerOn: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            /**
             * Android 12+：使用新API设置音频设备
             */
            val deviceType = if (isSpeakerOn) {
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
            /**
             * Android 12以下：使用旧API
             */
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = isSpeakerOn
        }
    }

    /**
     * 获取当前是否为扬声器模式
     *
     * @return true-扬声器模式，false-听筒模式
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
     * 停止播放并释放资源
     * 恢复之前保存的音频设置
     */
    fun stop() {
        /**
         * 恢复音频设置
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        audioManager.mode = savedAudioMode
        setSpeakerphoneOn(savedSpeakerState)

        /**
         * 释放MediaPlayer资源
         */
        val canStop = isPrepared
        isPrepared = false
        mediaPlayer?.run {
            try {
                if (canStop && isPlaying) {
                    stop()
                }
            } catch (_: Exception) {
                // 忽略停止时的异常
            } finally {
                release()
            }
        }
        mediaPlayer = null
    }
}
