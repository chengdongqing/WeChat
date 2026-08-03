package top.chengdongqing.wechat.feature.chat.ui.session.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.media.SoundTipPlayer
import top.chengdongqing.wechat.core.common.media.VoicePlayer
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.feature.chat.ui.session.input.voice.AudioFocusManager
import kotlin.time.Duration.Companion.milliseconds

/**
 * 音频播放管理器 - 封装所有音频播放相关逻辑
 */
class AudioPlaybackManager(
    context: Context,
    private val scope: CoroutineScope,
    private val soundTipPlayer: SoundTipPlayer,
    private val onPlaybackStateChanged: (VoicePlaybackState) -> Unit,
    private val onMessagePlayed: (String) -> Unit
) {
    private val audioFocusManager = AudioFocusManager(context)
    private val voicePlayer = VoicePlayer(context)

    private var currentPlayingId: String? = null
    private var progressJob: Job? = null
    private var speed = 1f

    fun togglePlay(
        messageId: String,
        localPath: String,
        messages: List<ChatMessage>,
        isSpeakerOn: Boolean
    ) {
        if (currentPlayingId == messageId) {
            if (voicePlayer.isPlaying) {
                voicePlayer.pause()
            } else {
                voicePlayer.resume()
            }
            publishState()
        } else {
            startPlaying(
                messageId = messageId,
                localPath = localPath,
                messages = messages,
                isSpeakerOn = isSpeakerOn,
                isContinuous = false
            )
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        voicePlayer.stop()
        audioFocusManager.abandonFocus()
        currentPlayingId = null
        speed = 1f
        onPlaybackStateChanged(VoicePlaybackState())
    }

    fun seekTo(messageId: String, fraction: Float) {
        if (currentPlayingId != messageId) return
        voicePlayer.seekTo((voicePlayer.duration * fraction.coerceIn(0f, 1f)).toInt())
        publishState()
    }

    fun toggleSpeed(messageId: String) {
        if (currentPlayingId != messageId || !voicePlayer.isPlaying) return
        speed = if (speed == 1f) 1.5f else 1f
        voicePlayer.setSpeed(speed)
        publishState()
    }

    fun setSpeakerOn(isSpeakerOn: Boolean) {
        if (currentPlayingId != null) {
            voicePlayer.setOutputMode(isSpeakerOn)
        }
    }

    fun release() {
        stop()
    }

    private fun startPlaying(
        messageId: String,
        localPath: String,
        messages: List<ChatMessage>,
        isSpeakerOn: Boolean,
        isContinuous: Boolean
    ) {
        // 首次播放时申请音频焦点
        if (!isContinuous) {
            audioFocusManager.requestFocus()
        }

        currentPlayingId = messageId
        speed = 1f
        publishState(isPlaying = true)
        onMessagePlayed(messageId)

        voicePlayer.play(localPath, isSpeakerOn, speed) {
            handlePlaybackCompleted(messageId, messages, isSpeakerOn)
        }
        startProgressUpdates()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && currentPlayingId != null) {
                publishState()
                delay(50.milliseconds)
            }
        }
    }

    private fun publishState(isPlaying: Boolean = voicePlayer.isPlaying) {
        onPlaybackStateChanged(
            VoicePlaybackState(
                messageId = currentPlayingId,
                positionMs = voicePlayer.currentPosition,
                durationMs = voicePlayer.duration,
                isPlaying = isPlaying,
                speed = speed
            )
        )
    }

    private fun handlePlaybackCompleted(
        messageId: String,
        messages: List<ChatMessage>,
        isSpeakerOn: Boolean
    ) {
        progressJob?.cancel()
        progressJob = null
        soundTipPlayer.play(R.raw.tip_voice_played)

        val nextVoice = findNextUnreadVoice(messageId, messages)
        if (nextVoice != null) {
            // 连续播放下一条
            scope.launch {
                onPlaybackStateChanged(VoicePlaybackState())
                delay(250.milliseconds)
                startPlaying(
                    messageId = nextVoice.id,
                    localPath = nextVoice.localPath,
                    messages = messages,
                    isSpeakerOn = isSpeakerOn,
                    isContinuous = true
                )
            }
        } else {
            // 没有更多消息，释放音频焦点
            audioFocusManager.abandonFocus()
            progressJob?.cancel()
            currentPlayingId = null
            speed = 1f
            onPlaybackStateChanged(VoicePlaybackState())
        }
    }

    private fun findNextUnreadVoice(
        currentMsgId: String,
        messages: List<ChatMessage>
    ): VoiceInfo? {
        val currentIndex = messages.indexOfFirst { it.id == currentMsgId }
        if (currentIndex == -1) return null

        // 从当前消息向更新的消息查找（index 越小，消息越新）
        for (i in (currentIndex - 1) downTo 0) {
            val message = messages[i]
            val content = message.content
            if (content is MessageContent.Voice && !content.isPlayed) {
                return VoiceInfo(message.id, content.localPath)
            }
        }
        return null
    }

    private data class VoiceInfo(val id: String, val localPath: String)
}

data class VoicePlaybackState(
    val messageId: String? = null,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val isPlaying: Boolean = false,
    val speed: Float = 1f
) {
    val progress: Float
        get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
}
