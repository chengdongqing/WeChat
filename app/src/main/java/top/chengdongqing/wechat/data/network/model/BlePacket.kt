package top.chengdongqing.wechat.data.network.model

import java.nio.ByteBuffer

object BlePacketType {
    const val JSON: Byte = 0x01       // JSON 消息头
    const val BINARY: Byte = 0x02     // 二进制数据（头像等）
    const val END: Byte = 0x03        // 传输结束标记
}

/**
 * Wire format（每个 BLE chunk）：
 * ┌──────┬──────────┬────────────┐
 * │ type │ length   │ body       │
 * │ 1 B  │ 2 bytes  │ N bytes    │
 * └──────┴──────────┴────────────┘
 * BLE MTU 通常 ≤ 512B，length 用 2 字节够用
 */
data class BlePacket(
    val type: Byte,
    val body: ByteArray = EMPTY_BODY
) {
    companion object {
        val EMPTY_BODY = ByteArray(0)

        fun json(data: ByteArray) = BlePacket(BlePacketType.JSON, data)
        fun binary(data: ByteArray) = BlePacket(BlePacketType.BINARY, data)
        fun end() = BlePacket(BlePacketType.END)

        fun fromBytes(bytes: ByteArray): BlePacket {
            require(bytes.size >= 3) { "无效包长度: ${bytes.size}" }
            val type = bytes[0]
            val length = ByteBuffer.wrap(bytes, 1, 2).short.toInt() and 0xFFFF
            val body = if (length > 0) bytes.copyOfRange(3, 3 + length) else ByteArray(0)
            return BlePacket(type, body)
        }
    }

    fun toBytes(): ByteArray {
        return ByteBuffer.allocate(3 + body.size).apply {
            put(type)
            putShort(body.size.toShort())
            put(body)
        }.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlePacket) return false
        return type == other.type && body.contentEquals(other.body)
    }

    override fun hashCode() = 31 * type.hashCode() + body.contentHashCode()
}