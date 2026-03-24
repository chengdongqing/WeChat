package top.chengdongqing.wechat.feature.call.domain.model

/**
 * 通话操作回调
 */
data class CallActions(
    val onAccept: () -> Unit = {},
    val onDecline: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onHangup: () -> Unit = {},
    val onToggleMic: () -> Unit = {},
    val onToggleSpeaker: () -> Unit = {},
    val onToggleVideo: () -> Unit = {},
    val onSwitchCamera: () -> Unit = {},
    val onSwapVideo: () -> Unit = {},
    val onToggleControlsVisibility: () -> Unit = {},
    val onMinimize: () -> Unit = {},
)