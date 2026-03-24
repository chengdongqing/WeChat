package top.chengdongqing.wechat.feature.contacts.domain.model

import top.chengdongqing.wechat.core.common.util.toMD5Bytes
import java.nio.ByteBuffer

/**
 * 发现信标 - 用于二维码加好友的数据格式
 *
 * 字节布局（共26字节）：
 * ┌──────────────┬───────────┬──────────┐
 * │ userId hash  │ timestamp │ checksum │
 * │   16 bytes   │  8 bytes  │  2 bytes │
 * └──────────────┴───────────┴──────────┘
 */
data class DiscoveryBeacon(
    val userIdHash: ByteArray,
    val timestamp: Long,
    val checksum: Short
) {
    companion object {
        const val BEACON_SIZE = 26
        private const val EXPIRY_MS = 5 * 60 * 1000L

        fun create(userId: String): DiscoveryBeacon {
            val hash = userId.toMD5Bytes()
            val timestamp = System.currentTimeMillis()
            return DiscoveryBeacon(hash, timestamp, computeChecksum(hash, timestamp))
        }

        fun fromByteArray(bytes: ByteArray): DiscoveryBeacon {
            require(bytes.size == BEACON_SIZE)
            return ByteBuffer.wrap(bytes).run {
                val hash = ByteArray(16).also { get(it) }
                val timestamp = long
                val checksum = short
                DiscoveryBeacon(hash, timestamp, checksum)
            }
        }

        fun computeChecksum(userIdHash: ByteArray, timestamp: Long): Short {
            var crc = 0xFFFF
            for (b in userIdHash) {
                crc = crc xor (b.toInt() and 0xFF)
                repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8408 else crc ushr 1 }
            }
            for (i in 0 until 8) {
                val b = ((timestamp ushr (i * 8)) and 0xFF).toInt()
                crc = crc xor b
                repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8408 else crc ushr 1 }
            }
            return (crc and 0xFFFF).toShort()
        }
    }

    fun toByteArray(): ByteArray = ByteBuffer.allocate(BEACON_SIZE).apply {
        put(userIdHash)
        putLong(timestamp)
        putShort(checksum)
    }.array()

    fun isValid(): Boolean {
        if (checksum != computeChecksum(userIdHash, timestamp)) return false
        return System.currentTimeMillis() - timestamp in 0..EXPIRY_MS
    }

    val userIdHashHex: String get() = userIdHash.toHexString()

    override fun equals(other: Any?) = other is DiscoveryBeacon
            && userIdHash.contentEquals(other.userIdHash)
            && timestamp == other.timestamp
            && checksum == other.checksum

    override fun hashCode() =
        31 * (31 * userIdHash.contentHashCode() + timestamp.hashCode()) + checksum
}