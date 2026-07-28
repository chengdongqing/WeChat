package top.chengdongqing.wechat.core.network.model

import android.app.NotificationManager

/**
 * 通知渠道
 */
enum class NotificationChannelConfig(
    val id: String,
    val title: String,
    val description: String,
    val importance: Int
) {
    P2P(
        id = "p2p_service",
        title = "基础通信服务",
        description = "保证消息收发、加好友等功能的运行",
        importance = NotificationManager.IMPORTANCE_NONE
    ),
    Message(
        id = "message",
        title = "新消息",
        description = "收到新消息时使用的通知类别",
        importance = NotificationManager.IMPORTANCE_HIGH
    ),
    Call(
        // 通知渠道的级别一经创建便不能由应用提升。使用新 ID，避免旧版本的
        // "call" 渠道已被系统或用户降级后，fullScreenIntent 永远不再触发。
        id = "incoming_call_v2",
        title = "音视频通话邀请通知",
        description = "收到音视频通话邀请时使用的通知类别",
        importance = NotificationManager.IMPORTANCE_MAX
    ),
    OngoingCall(
        // 使用新 ID，避免设备保留旧来电渠道的震动/悬浮设置。
        id = "ongoing_call_v2",
        title = "通话进行中",
        description = "显示通话时长以及静音、挂断控制",
        importance = NotificationManager.IMPORTANCE_DEFAULT
    )
}
