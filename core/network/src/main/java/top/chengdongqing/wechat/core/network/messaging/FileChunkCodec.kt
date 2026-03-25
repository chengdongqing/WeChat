package top.chengdongqing.wechat.core.network.messaging

import java.nio.ByteBuffer

/**
 * FILE_CHUNK 包体编解码
 */
object FileChunkCodec {
    // 头部固定长度：1字节(ID长度) + 8字节(Offset) = 9字节
    private const val HEADER_SIZE = Byte.SIZE_BYTES + Long.SIZE_BYTES

    /**
     * 格式：[2B id长度][8B offset][id 字节][data 实际数据]
     */
    fun encode(messageId: String, offset: Long, data: ByteArray): ByteArray {
        val idBytes = messageId.toByteArray(Charsets.UTF_8)
        require(idBytes.size <= 0xFF) {
            "messageId 过长: ${idBytes.size} bytes"
        }

        return ByteBuffer
            .allocate(Short.SIZE_BYTES + Long.SIZE_BYTES + idBytes.size + data.size)
            .putShort(idBytes.size.toShort())    // 2 字节 ID 长度
            .putLong(offset)                     // 8 字节偏移量 (新加)
            .put(idBytes)                        // messageId
            .put(data)                           // 分片数据
            .array()
    }

    /**
     * 解析出 (messageId, offset, data)
     */
    fun decode(body: ByteArray): Triple<String, Long, ByteArray> {
        // 基础长度校验：至少要能读出 Header
        require(body.size >= HEADER_SIZE) {
            "FILE_CHUNK body 过短: ${body.size}"
        }

        val buf = ByteBuffer.wrap(body)
        val idLength = buf.short.toInt() and 0xFF // 读取 1B 无符号长度
        val offset = buf.long                     // 读取 8B 偏移量

        // 剩余长度校验：防止读 ID 时越界
        require(idLength > 0 && buf.remaining() >= idLength) {
            "FILE_CHUNK 解析异常: idLen=$idLength, remaining=${buf.remaining()}"
        }

        val idBytes = ByteArray(idLength).also { buf.get(it) }
        val data = ByteArray(buf.remaining()).also { buf.get(it) }

        return Triple(String(idBytes, Charsets.UTF_8), offset, data)
    }
}