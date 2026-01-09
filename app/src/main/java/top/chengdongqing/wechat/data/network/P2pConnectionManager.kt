package top.chengdongqing.wechat.data.network

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import java.io.File

interface P2pConnectionManager {
    // 1. 发现的人：泛型化，向上转型为 P2PPeer
    val peers: StateFlow<List<P2PPeer>>

    // 2. 收到的消息
    val messageFlow: SharedFlow<MessageEnvelope>

    fun startDiscovery(myName: String)
    fun stopDiscovery()

    // 3. 建立链路：传入抽象的 Peer
    // 具体的实现类（如 WifiLanManager）会自己强转回 LanPeer 去拿 IP
    suspend fun connect(peer: P2PPeer): Boolean

    // 4. 发消息
    suspend fun sendText(peer: P2PPeer, text: String): Boolean

    // 发送文件
    suspend fun sendMedia(
        peer: P2PPeer,
        payload: ChatPayload.Media,
        file: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean

    fun disconnect(peer: P2PPeer)
}