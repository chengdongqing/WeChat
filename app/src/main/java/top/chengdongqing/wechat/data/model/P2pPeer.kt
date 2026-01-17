package top.chengdongqing.wechat.data.model

import android.net.wifi.p2p.WifiP2pDevice

/**
 * p2p节点
 */
interface P2PPeer {
    val id: String       // 唯一标识（UUID 或 MAC）
    val name: String     // 显示名称
    val lastSeen: Long    // 在线心跳
        get() = System.currentTimeMillis()
}

data class WifiLanPeer(
    override val id: String,
    override val name: String,
    val ip: String
) : P2PPeer

data class WifiDirectPeer(
    override val id: String,
    override val name: String,
    val mac: String?,
    val status: Int = WifiP2pDevice.UNAVAILABLE, // WifiP2pDevice.CONNECTED, .INVITED 等
    val ip: String? = null
) : P2PPeer

data class BluetoothPeer(
    override val id: String,
    override val name: String,
    val mac: String
) : P2PPeer