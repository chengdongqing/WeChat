package top.chengdongqing.wechat.features.chat.ui.session.peer

import top.chengdongqing.wechat.features.chat.domain.model.PeerDevice

/**
 * WiFi Direct 连接角色
 */
enum class WiFiDirectRole { None, Owner, Client }

/**
 * 设备列表页面 UI 状态
 */
data class PeerDeviceUiState(
    val isScanning: Boolean = false,
    val pairedDevices: List<PeerDevice> = emptyList(),
    val nearbyDevices: List<PeerDevice> = emptyList(),
    val connectingDeviceId: String? = null,
    val role: WiFiDirectRole = WiFiDirectRole.None,
    val error: String? = null
)
