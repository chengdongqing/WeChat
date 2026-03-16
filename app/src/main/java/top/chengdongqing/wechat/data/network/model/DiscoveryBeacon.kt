package top.chengdongqing.wechat.data.network.model

import top.chengdongqing.wechat.core.util.toMD5Bytes
import java.nio.ByteBuffer

/**
 * 发现信标 - 用于所有加好友方式的统一数据格式
 *
 * 字节布局（共26字节）：
 * ┌──────────────┬───────────┬──────────┐
 * │ userId hash  │ timestamp │ checksum │
 * │   16 bytes   │  8 bytes  │  2 bytes │
 * └──────────────┴───────────┴──────────┘
 */
data class DiscoveryBeacon(
    val userId: String,    // 序列化时哈希为16字节；fromByteArray 时为哈希的十六进制表示
    val timestamp: Long,   // 8字节，毫秒时间戳
    val checksum: Short    // 2字节，基于 userId + timestamp 的校验和
) {
    companion object {
        // 16(userId MD5) + 8(timestamp) + 2(checksum)
        const val BEACON_SIZE = 26

        /** 过期时长：5分钟 */
        private const val EXPIRY_MS = 5 * 60 * 1000L

        /**
         * 创建 Beacon（自动填充时间戳和校验和）
         */
        fun create(userId: String): DiscoveryBeacon {
            val timestamp = System.currentTimeMillis()
            val checksum = computeChecksum(userId, timestamp)
            return DiscoveryBeacon(userId, timestamp, checksum)
        }

        /**
         * 序列化为字节数组（26字节）
         */
        fun toByteArray(beacon: DiscoveryBeacon): ByteArray =
            ByteBuffer.allocate(BEACON_SIZE).apply {
                put(beacon.userId.toMD5Bytes())   // 16 bytes
                putLong(beacon.timestamp)           // 8 bytes
                putShort(beacon.checksum)           // 2 bytes
            }.array()

        /**
         * 从字节数组反序列化
         *
         * 注意：返回的 userId 是 MD5 的十六进制字符串，非原始 ID，
         * 原始 ID 需通过后续 P2P 连接获取。
         */
        fun fromByteArray(bytes: ByteArray): DiscoveryBeacon {
            require(bytes.size == BEACON_SIZE) {
                "Invalid beacon size: expected $BEACON_SIZE, got ${bytes.size}"
            }
            return ByteBuffer.wrap(bytes).run {
                val userIdHash = ByteArray(16).also { get(it) }.toHexString()
                val timestamp = long
                val checksum = short
                DiscoveryBeacon(userIdHash, timestamp, checksum)
            }
        }

        /**
         * 计算校验和（基于字节级运算，跨平台一致）
         */
        fun computeChecksum(userId: String, timestamp: Long): Short {
            val userIdBytes = userId.toByteArray(Charsets.UTF_8)
            var crc = 0xFFFF

            for (b in userIdBytes) {
                crc = crc xor (b.toInt() and 0xFF)
                repeat(8) {
                    crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8408
                    else crc ushr 1
                }
            }

            // 混入 timestamp 的每个字节
            for (i in 0 until 8) {
                val b = ((timestamp ushr (i * 8)) and 0xFF).toInt()
                crc = crc xor b
                repeat(8) {
                    crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8408
                    else crc ushr 1
                }
            }

            return (crc and 0xFFFF).toShort()
        }
    }

    /**
     * 校验信标完整性与时效性：
     * 1. checksum 匹配
     * 2. 时间戳在5分钟内
     */
    fun isValid(): Boolean {
        val expectedChecksum = computeChecksum(userId, timestamp)
        if (checksum != expectedChecksum) return false

        val age = System.currentTimeMillis() - timestamp
        return age in 0..EXPIRY_MS
    }
}