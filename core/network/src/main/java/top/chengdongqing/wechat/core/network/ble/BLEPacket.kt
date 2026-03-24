package top.chengdongqing.wechat.core.network.ble

import java.nio.ByteBuffer

object BLEPacketType {
    const val JSON: Byte = 0x01       // JSON message header
    const val BINARY: Byte = 0x02     // Binary payload (e.g. avatar)
    const val END: Byte = 0x03        // End-of-transfer marker
}

/**
 * Wire format for each BLE chunk:
 * ┌──────┬──────────┬────────────┐
 * │ type │ length   │ body       │
 * │ 1 B  │ 2 bytes  │ N bytes    │
 * └──────┴──────────┴────────────┘
 * BLE MTU is typically ≤ 512 B, so a 2-byte length field is sufficient.
 */
data class BLEPacket(
    val type: Byte,
    val body: ByteArray = EMPTY_BODY
) {
    companion object {
        val EMPTY_BODY = ByteArray(0)

        fun end() = BLEPacket(BLEPacketType.END)

        /**
         * Deserializes a [BLEPacket] from its raw byte representation.
         */
        fun fromBytes(bytes: ByteArray): BLEPacket {
            require(bytes.size >= 3) { "Invalid packet length: ${bytes.size}" }
            val type = bytes[0]
            val length = ByteBuffer.wrap(bytes, 1, 2).short.toInt() and 0xFFFF
            val body = if (length > 0) bytes.copyOfRange(3, 3 + length) else ByteArray(0)
            return BLEPacket(type, body)
        }
    }

    /**
     * Serializes this packet to its raw byte representation.
     */
    fun toBytes(): ByteArray {
        return ByteBuffer.allocate(3 + body.size).apply {
            put(type)
            putShort(body.size.toShort())
            put(body)
        }.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BLEPacket) return false
        return type == other.type && body.contentEquals(other.body)
    }

    override fun hashCode() = 31 * type.hashCode() + body.contentHashCode()
}