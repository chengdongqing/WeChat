package top.chengdongqing.wechat.data.network.model

import top.chengdongqing.wechat.data.network.config.TransferConfig
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Packet 类型常量
 *
 * 每个 Packet 的第一个字节标识其类型，接收端据此路由到对应处理逻辑。
 * Bit7（0x80）为加密标志位，与类型值 OR 运算，不占用独立类型槽位。
 *
 * Wire format：
 * ┌──────────┬──────┬────────────┐
 * │ length   │ type │ body       │
 * │ 4 bytes  │ 1 B  │ N bytes    │
 * └──────────┴──────┴────────────┘
 * length = 1（type）+ N（body）
 */
object PacketType {
    const val TEXT: Byte = 0x01          // 文本消息 (JSON)
    const val FILE_META: Byte = 0x02     // 媒体文件元数据 (JSON)，后续紧跟 N 个 FILE_CHUNK
    const val FILE_CHUNK: Byte = 0x03    // 媒体文件分片 (raw bytes)
    const val RECEIPT: Byte = 0x04       // 回执消息 (JSON)
    const val PING: Byte = 0x05          // 心跳探测，期望对端回 PONG
    const val PONG: Byte = 0x06          // 心跳响应
    const val HANDSHAKE: Byte = 0x07     // 握手包 (JSON)，连接建立后的第一个包
    const val SIGNALING: Byte = 0x08     // WebRTC 信令 (JSON)
    const val PROFILE_REQUEST: Byte = 0x09  // 拉取对方完整资料
    const val PROFILE_RESPONSE: Byte = 0x0A // 资料响应
    const val FILE_META_ACK: Byte = 0x0B   // 文件元数据应答
    const val FILE_CANCEL: Byte = 0x0C     // 取消文件传输
    const val FILE_PAUSE: Byte = 0x0D      // 暂停文件传输
    const val FILE_RESUME: Byte = 0x0E     // 继续文件传输

    /** 始终明文传输的类型，不做加密 */
    val PLAINTEXT_TYPES = setOf(
        HANDSHAKE,
        PING,
        PONG
    )

    /** 判断该类型是否携带加密标志 */
    fun isEncrypted(type: Byte) = (type.toInt() and 0x80) != 0

    /** 去掉加密标志，还原真实类型 */
    fun realType(type: Byte): Byte = (type.toInt() and 0x7F).toByte()

    /** 打上加密标志，得到加密后的类型字节 */
    fun encryptedType(type: Byte): Byte = (type.toInt() or 0x80).toByte()
}

/**
 * 网络数据包
 *
 * @property type 类型字节，见 [PacketType]；Bit7 为加密标志
 * @property body 载荷
 */
data class Packet(
    val type: Byte,
    val body: ByteArray = EMPTY_BODY
) {
    companion object {
        val EMPTY_BODY = ByteArray(0)

        /**
         * 在发送Ping时携带自己个人资料的版本号，方便更新
         */
        fun ping(profileVersion: Long, momentsVersion: Long = 0): Packet {
            val buffer = ByteBuffer.allocate(16)
            buffer.putLong(profileVersion)
            buffer.putLong(momentsVersion)

            return Packet(PacketType.PING, buffer.array())
        }

        fun pong() = Packet(PacketType.PONG)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Packet) return false
        return type == other.type && body.contentEquals(other.body)
    }

    override fun hashCode() = 31 * type.hashCode() + body.contentHashCode()
}

/**
 * Packet 写入器
 *
 * 基于 BufferedOutputStream（256KB）减少 write syscall。
 * header（length 4B + type 1B）与 body 在同一缓冲区内积攒，一次性推送到内核。
 *
 * 两种写入模式：
 * - [write]：写完立即 flush，适用于文本消息、回执、控制包等需要实时送达的场景
 * - [writeNoFlush]：写入不 flush，适用于 FILE_CHUNK 批量发送；
 *   buffer 满时自动 flush，block 结束后由调用方统一调用 [flush]
 *
 * 所有方法均 synchronized，支持多线程安全写入。
 */
class PacketWriter(outputStream: OutputStream) {
    private val out = DataOutputStream(
        BufferedOutputStream(outputStream, TransferConfig.STREAM_WRITE_BUFFER)
    )

    @Synchronized
    fun write(packet: Packet) {
        writeInternal(packet)
        out.flush()
    }

    @Synchronized
    fun writeNoFlush(packet: Packet) {
        writeInternal(packet)
    }

    @Synchronized
    fun flush() = out.flush()

    @Synchronized
    fun close() = runCatching { out.close() }

    private fun writeInternal(packet: Packet) {
        out.writeInt(1 + packet.body.size)
        out.writeByte(packet.type.toInt())
        out.write(packet.body)
    }
}

/**
 * Packet 读取器
 *
 * 基于 BufferedInputStream（256KB）预读，减少 read syscall。
 * 每次 [read] 阻塞直到读完一个完整 Packet
 * 包长度超出 [TransferConfig.MAX_PACKET_LENGTH] 时抛出异常，防止异常数据撑爆内存。
 */
class PacketReader(inputStream: InputStream) {
    private val input = DataInputStream(
        BufferedInputStream(inputStream, TransferConfig.STREAM_READ_BUFFER)
    )

    fun read(): Packet {
        val length = input.readInt()
        require(length in 1..TransferConfig.MAX_PACKET_LENGTH) { "非法包长度: $length" }

        val type = input.readByte()
        val bodyLength = length - 1
        val body = if (bodyLength > 0) ByteArray(bodyLength).also { input.readFully(it) }
        else Packet.EMPTY_BODY

        return Packet(type, body)
    }

    fun close() = runCatching { input.close() }
}