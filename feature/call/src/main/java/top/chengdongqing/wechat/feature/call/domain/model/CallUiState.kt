package top.chengdongqing.wechat.feature.call.domain.model

import android.content.Context
import top.chengdongqing.wechat.core.designsystem.R

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
    val duration: Long = 0,
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

    fun getStatusText(context: Context): String? = when (callState) {
        CallState.Outgoing -> context.getString(R.string.call_state_outgoing)
        CallState.Incoming -> context.getString(
            if (isVideoCall) R.string.call_state_incoming_video
            else R.string.call_state_incoming_voice
        )

        CallState.Connecting -> context.getString(R.string.call_state_connecting)
        CallState.Connected -> formatDuration(duration)
        CallState.Ended -> context.getString(
            when (hangupResult?.reason) {
                HangupReason.Normal -> if (hangupResult.isFromMe) R.string.call_state_ended_normal_by_me else R.string.call_state_ended_normal_by_other
                HangupReason.Declined -> if (isOutgoing) R.string.call_state_ended_declined_by_other else R.string.call_state_ended_declined_by_me
                HangupReason.Cancelled -> if (isOutgoing) R.string.call_state_ended_cancelled_by_me else R.string.call_state_ended_cancelled_by_other
                HangupReason.Timeout -> R.string.call_state_ended_timeout
                HangupReason.Busy -> R.string.call_state_ended_busy
                HangupReason.Offline -> R.string.call_state_ended_offline
                HangupReason.Error -> R.string.call_state_ended_error
                else -> R.string.call_state_ended
            }
        )

        CallState.Idle -> null
    }

    companion object {
        fun formatDuration(seconds: Long): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }
    }
}