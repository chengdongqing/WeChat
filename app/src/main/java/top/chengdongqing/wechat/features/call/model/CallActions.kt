package top.chengdongqing.wechat.features.call.model

/**
 * 通话操作回调（传给 UI 组件）
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