package top.chengdongqing.wechat.data.network.protocol

object SocketProtocol {
    const val TYPE_JSON: Byte = 0x01
    const val TYPE_BINARY: Byte = 0x02
}

sealed class SocketFrame {
    data class JsonFrame(val data: ByteArray) : SocketFrame() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as JsonFrame

            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    data class BinaryFrame(val messageId: String, val data: ByteArray) : SocketFrame() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BinaryFrame

            if (messageId != other.messageId) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = messageId.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}