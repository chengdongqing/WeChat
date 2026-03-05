package top.chengdongqing.wechat.features.settings.domain.model

/**
 * 通知显示方式
 */
enum class NotificationDisplay(val description: String) {
    HiddenAll("仅显示「你收到了1条消息」"),
    SenderOnly("显示朋友和群聊的名称"),
    SenderAndContent("显示朋友、群聊名称及消息内容");
}