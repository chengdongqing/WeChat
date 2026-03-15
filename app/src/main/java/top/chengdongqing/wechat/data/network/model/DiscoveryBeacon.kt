package top.chengdongqing.wechat.data.network.model

import top.chengdongqing.wechat.core.util.toMD5Bytes
import java.nio.ByteBuffer

/**
 * 发现信标 - 用于所有加好友方式的统一数据格式
 */
data class DiscoveryBeacon(
    val userId: String,              // 原始用户ID（序列化时会被哈希为16字节）
    val timestamp: Long,             // 8字节
    val checksum: Short              // 2字节
) {

    companion object {
        private const val BEACON_SIZE = 30  // 16 + 4 + 8 + 2

        /**
         * 创建用于序列化的Beacon
         */
        fun create(userId: String): DiscoveryBeacon {
            val timestamp = System.currentTimeMillis()
            val beacon = DiscoveryBeacon(
                userId = userId,
                timestamp = timestamp,
                checksum = 0
            )

            // 计算校验和
            val checksum = calculateChecksum(beacon)
            return beacon.copy(checksum = checksum)
        }

        /**
         * 序列化为字节数组
         */
        fun toByteArray(beacon: DiscoveryBeacon): ByteArray {
            val buffer = ByteBuffer.allocate(BEACON_SIZE)

            // userId (16 bytes) - 使用MD5哈希
            buffer.put(beacon.userId.toMD5Bytes())

            // timestamp (8 bytes)
            buffer.putLong(beacon.timestamp)

            // checksum (2 bytes)
            buffer.putShort(beacon.checksum)

            return buffer.array()
        }

        /**
         * 从字节数组反序列化
         * 注意：返回的 userId 是哈希值（32位十六进制），不是原始ID
         */
        fun fromByteArray(bytes: ByteArray): DiscoveryBeacon {
            require(bytes.size == BEACON_SIZE) { "Invalid beacon size: ${bytes.size}" }

            val buffer = ByteBuffer.wrap(bytes)

            // 读取 userId hash (16 bytes)
            val userIdHash = ByteArray(16)
            buffer.get(userIdHash)

            val timestamp = buffer.long
            val checksum = buffer.short

            // 返回的userId是哈希值的十六进制表示
            // 真实的userId需要通过P2P连接获取
            return DiscoveryBeacon(
                userId = userIdHash.toHexString(),
                timestamp = timestamp,
                checksum = checksum
            )
        }

        /**
         * 计算校验和
         */
        fun calculateChecksum(beacon: DiscoveryBeacon): Short {
            val data = "${beacon.userId}${beacon.timestamp}"
            return (data.hashCode() and 0xFFFF).toShort()
        }
    }

    /**
     * 验证信标有效性
     */
    fun isValid(): Boolean {
        // 检查时间戳（5分钟过期）
        val age = System.currentTimeMillis() - timestamp
        return age <= 5 * 60 * 1000
    }
}