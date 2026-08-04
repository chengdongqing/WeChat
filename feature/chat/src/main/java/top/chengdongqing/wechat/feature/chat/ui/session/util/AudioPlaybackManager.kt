package top.chengdongqing.wechat.feature.chat.ui.session.util

import android.content.Context
import android.os.SystemClock
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
    private var currentExpectedDurationMs = 0
    private var logicalPositionMs = 0f
    private var lastProgressUpdateMs = 0L
    private var progressJob: Job? = null
    private var speed = 1f

    fun togglePlay(
        messageId: String,
        localPath: String,
        expectedDurationMs: Long,
        messages: List<ChatMessage>,
        isSpeakerOn: Boolean
    ) {
        if (currentPlayingId == messageId) {
            if (voicePlayer.isPlaying) {
                updateLogicalPosition(isPlaying = true)
                voicePlayer.pause()
            } else {
                voicePlayer.resume()
            }
            lastProgressUpdateMs = SystemClock.elapsedRealtime()
            publishState()
        } else {
            startPlaying(
                messageId = messageId,
                localPath = localPath,
                expectedDurationMs = expectedDurationMs,
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
        currentExpectedDurationMs = 0
        logicalPositionMs = 0f
        lastProgressUpdateMs = 0L
        speed = 1f
        onPlaybackStateChanged(VoicePlaybackState())
    }

    fun seekTo(messageId: String, fraction: Float) {
        if (currentPlayingId != messageId) return
        val safeFraction = fraction.coerceIn(0f, 1f)
        logicalPositionMs = currentExpectedDurationMs * safeFraction
        lastProgressUpdateMs = SystemClock.elapsedRealtime()
        voicePlayer.seekTo((voicePlayer.duration * safeFraction).toInt())
        publishState()
    }

    fun toggleSpeed(messageId: String) {
        if (currentPlayingId != messageId || !voicePlayer.isPlaying) return
        updateLogicalPosition(isPlaying = true)
        speed = if (speed == 1f) 1.5f else 1f
        voicePlayer.setSpeed(speed)
        lastProgressUpdateMs = SystemClock.elapsedRealtime()
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
        expectedDurationMs: Long,
        messages: List<ChatMessage>,
        isSpeakerOn: Boolean,
        isContinuous: Boolean
    ) {
        // 首次播放时申请音频焦点
        if (!isContinuous) {
            audioFocusManager.requestFocus()
        }

        currentPlayingId = messageId
        currentExpectedDurationMs = expectedDurationMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        logicalPositionMs = 0f
        lastProgressUpdateMs = SystemClock.elapsedRealtime()
        speed = 1f
        // 播放器可能仍持有上一条语音结束时的位置，先发布明确的零进度，
        // 避免新气泡在异步 prepare 完成前短暂显示为 100%。
        onPlaybackStateChanged(
            VoicePlaybackState(
                messageId = messageId,
                positionMs = 0,
                durationMs = currentExpectedDurationMs,
                isPlaying = true,
                speed = speed
            )
        )
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
        updateLogicalPosition(isPlaying)
        val positionMs = if (currentExpectedDurationMs > 0) {
            logicalPositionMs.toInt().coerceAtMost(currentExpectedDurationMs)
        } else {
            voicePlayer.currentPosition
        }
        onPlaybackStateChanged(
            VoicePlaybackState(
                messageId = currentPlayingId,
                positionMs = positionMs,
                durationMs = currentExpectedDurationMs.takeIf { it > 0 }
                    ?: voicePlayer.duration,
                isPlaying = isPlaying,
                speed = speed
            )
        )
    }

    /**
     * 部分设备生成的 M4A 可以正常播放，但 MediaPlayer 的时间轴会直接跳到末尾。
     * 使用单调时钟维护 UI 进度，避免依赖容器时间戳；倍速播放时按速度同步推进。
     */
    private fun updateLogicalPosition(isPlaying: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (isPlaying && lastProgressUpdateMs > 0L) {
            logicalPositionMs += (now - lastProgressUpdateMs) * speed
        }
        lastProgressUpdateMs = now
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
                    expectedDurationMs = nextVoice.durationMs,
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
            currentExpectedDurationMs = 0
            logicalPositionMs = 0f
            lastProgressUpdateMs = 0L
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
                return VoiceInfo(message.id, content.localPath, content.duration)
            }
        }
        return null
    }

    private data class VoiceInfo(
        val id: String,
        val localPath: String,
        val durationMs: Long
    )
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
