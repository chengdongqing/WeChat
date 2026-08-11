package top.chengdongqing.wechat.core.qrcode

/**
 * 二维码类型
 */
sealed class QRCodeType {
    /**
     * 添加好友
     */
    data class AddFriend(val beaconBase64: String) : QRCodeType()

    /**
     * 加入群聊
     */
    data class JoinGroup(val groupId: String) : QRCodeType()

    /**
     * 网页链接
     */
    data class WebUrl(val url: String) : QRCodeType()

    /**
     * 普通文本
     */
    data class PlainText(val text: String) : QRCodeType()
}
