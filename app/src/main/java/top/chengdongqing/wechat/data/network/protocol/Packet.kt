package top.chengdongqing.wechat.data.network.protocol

import top.chengdongqing.wechat.data.network.config.TransferConfig
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * 消息类型常量
 *
 * 每个 Packet 的第一个字节标识其类型，用于接收端路由到对应的处理逻辑。
 *
 * 传输格式 (wire format):
 * ┌──────────┬──────┬────────────┐
 * │ length   │ type │ body       │
 * │ 4 bytes  │ 1 b  │ N bytes    │
 * └──────────┴──────┴────────────┘
 * length = 1 (type) + N (body)
 *
 * 特例: PING / PONG 的 body 为空，length = 1。
 */
object PacketType {
    /** JSON 文本消息 */
    const val TEXT: Byte = 0x01

    /** 媒体文件元数据 (JSON)，后续紧跟 N 个 FILE_CHUNK */
    const val FILE_META: Byte = 0x02

    /** 媒体文件分片 (raw bytes) */
    const val FILE_CHUNK: Byte = 0x03

    /** 送达/已读回执 (JSON) */
    const val ACK: Byte = 0x04

    /** 心跳 Ping，期望对端回复 PONG */
    const val PING: Byte = 0x05

    /** 心跳 Pong，对 PING 的响应 */
    const val PONG: Byte = 0x06

    /** 握手包 (JSON)，连接建立后的第一个包 */
    const val HANDSHAKE: Byte = 0x07

    /** 信令消息 (JSON)，用于音视频通话等 */
    const val SIGNALING: Byte = 0x08

    /** 已读回执 (JSON) */
    const val READ_RECEIPT: Byte = 0x09
}

/**
 * 网络数据包
 *
 * @property type 消息类型，见 [PacketType]
 * @property body 载荷数据；PING/PONG 时为空数组
 */
data class Packet(
    val type: Byte,
    val body: ByteArray = EMPTY_BODY
) {
    companion object {
        val EMPTY_BODY = ByteArray(0)

        /** 快捷构造 Ping 包 */
        fun ping() = Packet(PacketType.PING)

        /** 快捷构造 Pong 包 */
        fun pong() = Packet(PacketType.PONG)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Packet) return false
        return type == other.type && body.contentEquals(other.body)
    }

    override fun hashCode(): Int = 31 * type.hashCode() + body.contentHashCode()
}

/**
 * 高吞吐 Packet 写入器
 *
 * 性能优化:
 * 1. BufferedOutputStream (256KB) 合并小写入，减少 write syscall
 * 2. header (4+1 bytes) 和 body 在同一个 buffer 中积攒，一次性刷到内核
 * 3. FILE_CHUNK 批量写入时可调用 [writeBatch] 避免每片都 flush
 * 4. synchronized 保证原子性
 */
class PacketWriter(outputStream: OutputStream) {
    private val buffered = BufferedOutputStream(outputStream, TransferConfig.STREAM_WRITE_BUFFER)
    private val out = DataOutputStream(buffered)

    /**
     * 写入单个 Packet 并立即 flush
     *
     * 适用于: 文本消息、回执、Ping/Pong 等需要立即送达的小包。
     */
    @Synchronized
    fun write(packet: Packet) {
        writeInternal(packet)
        out.flush()
    }

    /**
     * 写入 Packet 但不 flush（用于批量写入场景）
     *
     * 适用于: FILE_CHUNK 批量发送。
     * 调用方负责在合适时机调用 [flush]。
     * buffer 满时 BufferedOutputStream 会自动 flush。
     */
    @Synchronized
    fun writeNoFlush(packet: Packet) {
        writeInternal(packet)
    }

    @Synchronized
    fun flush() {
        out.flush()
    }

    private fun writeInternal(packet: Packet) {
        val length = 1 + packet.body.size
        out.writeInt(length)
        out.writeByte(packet.type.toInt())
        out.write(packet.body)
    }

    @Synchronized
    fun close() {
        runCatching { out.close() }
    }
}

/**
 * 高吞吐 Packet 读取器
 *
 * BufferedInputStream (256KB) 预读，减少 read syscall。
 * 正确处理 bodyLength = 0（PING/PONG）。
 */
class PacketReader(inputStream: InputStream) {
    private val buffered = BufferedInputStream(inputStream, TransferConfig.STREAM_READ_BUFFER)
    private val input = DataInputStream(buffered)

    fun read(): Packet {
        val length = input.readInt()

        require(length in 1..TransferConfig.MAX_PACKET_LENGTH) {
            "非法包长度: $length"
        }

        val type = input.readByte()
        val bodyLength = length - 1

        val body = if (bodyLength > 0) {
            ByteArray(bodyLength).also { input.readFully(it) }
        } else {
            Packet.EMPTY_BODY
        }

        return Packet(type, body)
    }

    fun close() {
        runCatching { input.close() }
    }
}