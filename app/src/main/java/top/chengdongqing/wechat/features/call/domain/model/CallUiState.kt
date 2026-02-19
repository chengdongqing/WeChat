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
    val isPeerMicOn: Boolean = true,
    val isPeerSpeakerOn: Boolean = false,
    val isPeerVideoOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isVideoSwapped: Boolean = false,
    val isControlsVisible: Boolean = true,
    val duration: Int = 0,
    val hangupResult: HangupResult? = null
) {
    val isCallActive: Boolean get() = callState == CallState.Connected
    val isVideoCall: Boolean get() = callType.isVideoCall
    val isVideoCallActive: Boolean get() = isVideoCall && isCallActive

    // 是否应该显示全屏的本地预览（呼叫中且是视频通话）
    val showFullScreenLocalPreview: Boolean
        get() = isVideoCall && !isCallActive && isVideoOn && callState != CallState.Ended

    // 是否应该显示远程视频流
    val showRemoteVideo: Boolean
        get() = isVideoCallActive && isPeerVideoOn

    // 是否应该显示悬浮预览小窗
    val showFloatingWindow: Boolean
        get() = isVideoCallActive && isVideoOn

    // 是否应该在屏幕中央显示对方的大头像（视频通话但对方关了摄像头）
    val showCenterAvatar: Boolean
        get() = isVideoCallActive && !isPeerVideoOn

    // 是否在顶层显示用户信息（名字、状态文字等）
    val showTopUserInfo: Boolean
        get() = !isVideoCall || !isCallActive || !isPeerVideoOn

    // 是否允许切换全屏（只有视频接通后才允许点击大背景隐藏控件）
    val canToggleControls: Boolean get() = isVideoCallActive

    fun getStatusText(): String = when (callState) {
        CallState.Outgoing -> "等待对方接受邀请..."
        CallState.Incoming -> if (isVideoCall) "邀请你视频通话..." else "邀请你语音通话..."
        CallState.Connecting -> "连接中..."
        CallState.Connected -> formatDuration(duration)
        CallState.Ended -> when (hangupResult?.reason) {
            HangupReason.Normal -> if (hangupResult.isFromMe) "已挂断，通话结束" else "对方已挂断，通话结束"
            HangupReason.Declined -> if (isOutgoing) "对方拒绝了你的通话请求" else "已拒绝了对方的通话请求"
            HangupReason.Timeout -> "对方无应答"
            HangupReason.Busy -> "对方忙线中"
            HangupReason.Offline -> "对方不在线"
            HangupReason.Error -> "通话异常"
            else -> "通话结束"
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