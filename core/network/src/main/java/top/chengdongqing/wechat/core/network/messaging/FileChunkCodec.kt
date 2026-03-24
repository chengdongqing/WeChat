package top.chengdongqing.wechat.core.network.messaging

import java.nio.ByteBuffer

/**
 * FILE_CHUNK 包体编解码
 *
 * 格式：[2字节 messageId 长度（无符号 Short）][messageId UTF-8 字节][data 实际分片数据]
 */
object FileChunkCodec {

    /**
     * 将 [messageId] 和原始 [data] 打包成一个 FILE_CHUNK body
     */
    fun encode(messageId: String, data: ByteArray): ByteArray {
        val idBytes = messageId.toByteArray(Charsets.UTF_8)
        require(idBytes.size <= UShort.MAX_VALUE.toInt()) {
            "messageId 过长: ${idBytes.size} bytes"
        }

        return ByteBuffer
            .allocate(Short.SIZE_BYTES + idBytes.size + data.size)
            .putShort(idBytes.size.toShort())    // 2 字节长度头
            .put(idBytes)                        // messageId
            .put(data)                           // 分片数据
            .array()
    }

    /**
     * 从 FILE_CHUNK body 中解析出 messageId 与实际分片数据
     */
    fun decode(body: ByteArray): Pair<String, ByteArray> {
        require(body.size >= Short.SIZE_BYTES) {
            "FILE_CHUNK body 过短: ${body.size}"
        }

        val buf = ByteBuffer.wrap(body)
        val idLength = buf.short.toInt() and 0xFFFF   // 转为无符号

        require(idLength > 0 && buf.remaining() >= idLength) {
            "messageId 长度非法: idLength=$idLength, remaining=${buf.remaining()}"
        }

        val idBytes = ByteArray(idLength).also { buf.get(it) }
        val data = ByteArray(buf.remaining()).also { buf.get(it) }

        return String(idBytes, Charsets.UTF_8) to data
    }
}