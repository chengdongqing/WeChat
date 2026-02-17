package top.chengdongqing.wechat.features.call.domain.model

/**
 * 通话 UI 状态
 */
data class CallUiState(
    val callState: CallState = CallState.Idle,
    val callType: CallType = CallType.Voice,
    val callId: String = "",
    val peerId: String = "",
    val peerName: String = "",
    val peerAvatar: String? = null,
    val isOutgoing: Boolean = false,
    val isMicOn: Boolean = true,
    val isSpeakerOn: Boolean = false,
    val isVideoOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val duration: Int = 0,
    val endReason: HangupReason? = null
) {
    val isCallActive: Boolean get() = callState == CallState.Connected
    val isVideoCall: Boolean get() = callType.isVideoCall
    val isVideoCallActive: Boolean get() = isVideoCall && isCallActive

    fun getStatusText(): String = when (callState) {
        CallState.Outgoing -> "等待对方接受邀请..."
        CallState.Incoming -> if (isVideoCall) "邀请你视频通话..." else "邀请你语音通话..."
        CallState.Connecting -> "连接中..."
        CallState.Connected -> formatDuration(duration)
        CallState.Ended -> when (endReason) {
            HangupReason.Normal -> "通话结束"
            HangupReason.Declined -> if (isOutgoing) "对方已拒绝" else "已拒绝"
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