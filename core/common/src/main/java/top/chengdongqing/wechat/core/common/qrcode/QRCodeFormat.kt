package top.chengdongqing.wechat.core.common.qrcode

/**
 * 二维码格式定义
 */
object QRCodeFormat {
    private const val APP_DOMAIN = "wechat.local"

    /**
     * 生成添加好友的二维码内容
     * 格式: https://wechat.local/u/(base64)
     */
    fun generateAddFriendQRCode(beaconBase64: String): String {
        return "https://$APP_DOMAIN/u/$beaconBase64"
    }

    /**
     * 解析二维码内容
     */
    fun parseQRCode(content: String): QRCodeType {
        return when {
            // 添加好友二维码
            content.startsWith("https://$APP_DOMAIN/u/") -> {
                val base64 = content.removePrefix("https://$APP_DOMAIN/u/")
                QRCodeType.AddFriend(base64)
            }

            //加入群聊二维码
            content.startsWith("https://$APP_DOMAIN/g/") -> {
                val groupId = content.removePrefix("https://$APP_DOMAIN/g/")
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

