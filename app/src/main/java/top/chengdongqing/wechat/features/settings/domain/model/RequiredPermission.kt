package top.chengdongqing.wechat.features.settings.domain.model

/**
 * 系统权限
 */
enum class RequiredPermission(
    val label: String,
    val description: String
) {
    Location(
        "位置信息",
        "用于搜索周边的 Wi-Fi 节点及蓝牙设备等。"
    ),
    Microphone(
        "麦克风",
        "用于录制语音消息或进行实时通话等。"
    ),
    Camera(
        "相机",
        "用于拍摄照片、视频以及实时通话、扫描二维码等。"
    ),
    NFC(
        "NFC",
        "用于通过触碰快速建立连接或交换名片等。"
    ),
    Bluetooth(
        "蓝牙",
        "用于在无网络环境下搜索并连接周边好友等。"
    ),
    WiFi(
        "Wi-Fi",
        "用于局域网内的高速文件传输与消息同步等。"
    )
}