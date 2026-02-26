package top.chengdongqing.wechat.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_info")
data class ConnectionInfoEntity(
    @PrimaryKey
    val userId: String,                 // 用户ID

    val connectionType: ConnectionType, // 连接类型

    // WiFi LAN 信息
    val ipAddress: String? = null,      // IP地址
    val port: Int? = null,              // 端口号
    val serviceName: String? = null,    // NSD服务名

    // WiFi Direct 信息
    val macAddress: String? = null,     // MAC地址
    val p2pDeviceName: String? = null,  // P2P设备名

    // Bluetooth 信息
    val bluetoothAddress: String? = null, // 蓝牙地址
    val bluetoothName: String? = null,    // 蓝牙名称

    val isOnline: Boolean = false,      // 是否在线
    val lastSeen: Long,                 // 最后在线时间

    val priority: Int = 0,              // 连接优先级（0最高）

    @Embedded
    val audit: EntityAudit = EntityAudit()
)

enum class ConnectionType {
    WiFiLan,       // WiFi局域网
    WiFiDirect,    // WiFi直连
    Bluetooth      // 蓝牙
}