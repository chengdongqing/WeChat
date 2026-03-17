package top.chengdongqing.wechat.core.qrcode

import top.chengdongqing.wechat.features.contacts.domain.model.Contact

/**
 * 二维码处理结果
 */
sealed class QRCodeResult {
    /**
     * 添加好友成功
     */
    data class AddFriend(val contact: Contact) : QRCodeResult()

    /**
     * 加入群聊
     */
    data class JoinGroup(val groupId: String) : QRCodeResult()

    /**
     * 打开网页
     */
    data class OpenUrl(val url: String) : QRCodeResult()

    /**
     * 显示文本
     */
    data class ShowText(val text: String) : QRCodeResult()

    /**
     * 错误
     */
    data class Error(val message: String) : QRCodeResult()
}