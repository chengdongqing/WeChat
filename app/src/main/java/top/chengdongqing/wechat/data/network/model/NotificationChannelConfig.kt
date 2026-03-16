package top.chengdongqing.wechat.data.network.model

import android.app.NotificationManager
import top.chengdongqing.wechat.R

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
        title = "${R.string.app_name} 通信服务",
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
        id = "call",
        title = "音视频通话邀请通知",
        description = "收到音视频通话邀请时使用的通知类别",
        importance = NotificationManager.IMPORTANCE_MAX
    )
}