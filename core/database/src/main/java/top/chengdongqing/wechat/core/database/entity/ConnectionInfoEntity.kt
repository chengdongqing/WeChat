package top.chengdongqing.wechat.core.database.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "connection_info")
data class ConnectionInfoEntity(
    @PrimaryKey
    val userId: String,                 // 用户ID

    // WiFi LAN 信息
    val lanIpAddress: String? = null,      // IP地址
    val lanPort: Int? = null,              // 端口号
    val lanServiceName: String? = null,    // NSD服务名

    // WiFi Direct 信息
    val p2pMacAddress: String? = null,     // P2P MAC地址
    val p2pDeviceName: String? = null,     // P2P设备名

    // Bluetooth 信息
    val bluetoothAddress: String? = null, // 蓝牙地址
    val bluetoothName: String? = null,    // 蓝牙名称

    val isOnline: Boolean = false,      // 是否在线
    val lastSeen: Long,                 // 最后在线时间

    @Embedded
    val audit: EntityAudit = EntityAudit()
) {
    fun mergeWith(existing: ConnectionInfoEntity): ConnectionInfoEntity {
        return this.copy(
            lanIpAddress = lanIpAddress ?: existing.lanIpAddress,
            lanPort = lanPort ?: existing.lanPort,
            lanServiceName = lanServiceName ?: existing.lanServiceName,
            p2pMacAddress = p2pMacAddress ?: existing.p2pMacAddress,
            p2pDeviceName = p2pDeviceName ?: existing.p2pDeviceName,
            bluetoothAddress = bluetoothAddress ?: existing.bluetoothAddress,
            bluetoothName = bluetoothName ?: existing.bluetoothName,
            audit = audit.copy(
                createdAt = existing.audit.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}