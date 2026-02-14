package top.chengdongqing.wechat.data.network.socket

import kotlinx.coroutines.channels.Channel
import top.chengdongqing.wechat.data.network.protocol.SocketFrame
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

data class SocketConnection(
    val userId: String,
    val socket: Socket,
    val inputStream: DataInputStream,
    val outputStream: DataOutputStream,
    val frameChannel: Channel<SocketFrame> = Channel(Channel.UNLIMITED),
) {
    fun close() {
        runCatching {
            frameChannel.close()
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}