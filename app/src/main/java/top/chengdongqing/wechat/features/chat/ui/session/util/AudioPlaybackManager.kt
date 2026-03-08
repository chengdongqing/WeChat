package top.chengdongqing.wechat.features.chat.ui.session.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.media.VoicePlayer
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.input.voice.AudioFocusManager

/**
 * 音频播放管理器 - 封装所有音频播放相关逻辑
 */
class AudioPlaybackManager(
    context: Context,
    private val soundTipPlayer: SoundTipPlayer,
    private val onPlayingStateChanged: (String?) -> Unit,
    private val onMessagePlayed: (String) -> Unit
) {
    private val audioFocusManager = AudioFocusManager(context)
    private val voicePlayer = VoicePlayer(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentPlayingId: String? = null

    fun togglePlay(
        messageId: String,
        localPath: String,
        messages: List<ChatMessage>,
        isSpeakerOn: Boolean
    ) {
        if (currentPlayingId == messageId) {
            stop()
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
        voicePlayer.stop()
        audioFocusManager.abandonFocus()
        currentPlayingId = null
        onPlayingStateChanged(null)
    }

    fun release() {
        stop()
        scope.cancel()
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
        onPlayingStateChanged(messageId)
        onMessagePlayed(messageId)

        voicePlayer.play(localPath, isSpeakerOn) {
            handlePlaybackCompleted(messageId, messages, isSpeakerOn)
        }
    }

    private fun handlePlaybackCompleted(
        messageId: String,
        messages: List<ChatMessage>,
        isSpeakerOn: Boolean
    ) {
        soundTipPlayer.play(R.raw.play_completed)

        val nextVoice = findNextUnreadVoice(messageId, messages)
        if (nextVoice != null) {
            // 连续播放下一条
            scope.launch {
                onPlayingStateChanged(null)
                delay(250)
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
            currentPlayingId = null
            onPlayingStateChanged(null)
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