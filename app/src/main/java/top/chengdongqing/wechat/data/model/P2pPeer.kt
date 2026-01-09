package top.chengdongqing.wechat.data.model

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

data class BluetoothPeer(
    override val id: String,
    override val name: String,
    val macAddress: String, // 蓝牙特有的 MAC 地址
    val isBonded: Boolean = false   // 可选：记录配对状态
) : P2PPeer