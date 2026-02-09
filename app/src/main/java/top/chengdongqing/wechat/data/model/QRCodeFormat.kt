package top.chengdongqing.wechat.data.model

/**
 * 二维码格式定义
 */
object QRCodeFormat {
    private const val APP_DOMAIN = "wechat.local"

    /**
     * 生成添加好友的二维码内容
     * 格式: https://wechat.local/add/(base64)
     */
    fun generateAddFriendQRCode(beaconBase64: String): String {
        return "https://$APP_DOMAIN/add/$beaconBase64"
    }

    /**
     * 解析二维码内容
     */
    fun parseQRCode(content: String): QRCodeType {
        return when {
            // 添加好友二维码
            content.startsWith("https://$APP_DOMAIN/add/") -> {
                val base64 = content.removePrefix("https://$APP_DOMAIN/add/")
                QRCodeType.AddFriend(base64)
            }

            //加入群聊二维码
            content.startsWith("https://$APP_DOMAIN/group/") -> {
                val groupId = content.removePrefix("https://$APP_DOMAIN/group/")
                QRCodeType.JoinGroup(groupId)
            }

            // HTTP/HTTPS 链接
            content.startsWith("http://") || content.startsWith("https://") -> {
                QRCodeType.WebUrl(content)
            }

            // 普通文本
            else -> {
                QRCodeType.PlainText(content)
            }
        }
    }
}

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