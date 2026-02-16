package top.chengdongqing.wechat.features.call.data

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.features.chat.domain.model.CallType

@Serializable
enum class HangupReason {
    Normal,
    Declined,
    Timeout,
    Busy,
    Error
}

// ==================== 通话状态 ====================

enum class CallState {
    Idle, Outgoing, Incoming, Connecting, Connected, Ended
}

/**
 * 通话 UI 状态
 *
 * 直接驱动 CallScreen，包含所有 UI 需要的信息。
 */
data class CallUiState(
    val callState: CallState = CallState.Idle,
    val callType: CallType = CallType.Voice,
    val callId: String = "",
    val peerId: String = "",
    val peerName: String = "",
    val peerAvatar: String? = null,
    val isMicOn: Boolean = true,
    val isSpeakerOn: Boolean = false,
    val isVideoOff: Boolean = false,
    val isFrontCamera: Boolean = true,
    val duration: Int = 0,
    val endReason: HangupReason? = null
) {
    val isRinging: Boolean get() = callState == CallState.Incoming
    val isCallActive: Boolean get() = callState == CallState.Connected
    val isVideoCall: Boolean get() = callType.isVideoCall
    val shouldShowRemoteVideo: Boolean get() = isVideoCall && isCallActive
    val shouldShowLocalPreview: Boolean
        get() = isVideoCall && callState in setOf(
            CallState.Outgoing, CallState.Connecting, CallState.Connected
        )

    fun getStatusText(): String = when (callState) {
        CallState.Outgoing -> "正在等待对方接受邀请..."
        CallState.Incoming -> if (isVideoCall) "视频通话邀请" else "语音通话邀请"
        CallState.Connecting -> "连接中..."
        CallState.Connected -> formatDuration(duration)
        CallState.Ended -> when (endReason) {
            HangupReason.Normal -> "通话结束"
            HangupReason.Declined -> "对方已拒绝"
            HangupReason.Timeout -> "对方无应答"
            HangupReason.Busy -> "对方忙线中"
            HangupReason.Error -> "通话异常"
            null -> "通话结束"
        }

        CallState.Idle -> ""
    }

    companion object {
        fun formatDuration(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }
    }
}

/**
 * 通话操作回调（传给 UI 组件）
 */
data class CallActions(
    val onAccept: () -> Unit = {},
    val onReject: () -> Unit = {},
    val onHangup: () -> Unit = {},
    val onToggleMic: () -> Unit = {},
    val onToggleSpeaker: () -> Unit = {},
    val onSwitchCamera: () -> Unit = {},
    val onToggleVideo: () -> Unit = {},
    val onMinimize: () -> Unit = {}
)