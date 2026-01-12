package top.chengdongqing.wechat.data.network

import kotlinx.coroutines.flow.StateFlow
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import java.io.File

/**
 * 点对点连接管理器
 */
interface P2pConnectionManager {
    /**
     * 设备列表流
     */
    val peers: StateFlow<List<P2PPeer>>

    /**
     * 开始搜索设备
     */
    fun startDiscovery()

    /**
     * 停止搜素设备
     */
    fun stopDiscovery()

    /**
     * 连接设备
     */
    suspend fun connect(peer: P2PPeer): Boolean

    /**
     * 断开连接
     */
    fun disconnect(peer: P2PPeer)

    /**
     * 开始接收消息
     */
    fun startMessageServer()

    /**
     * 停止接收消息
     */
    fun stopMessageServer()

    /**
     * 发送文本消息
     */
    suspend fun sendText(peer: P2PPeer, envelope: MessageEnvelope): Boolean

    /**
     * 发送媒体消息
     */
    suspend fun sendMedia(
        peer: P2PPeer,
        envelope: MessageEnvelope,
        file: File,
        onProgress: suspend (Float) -> Unit // 发送进度回调
    ): Boolean

    /**
     * 直接发送文本数据到指定IP设备
     */
    suspend fun sendPayload(targetIp: String, payload: ChatPayload): Boolean
}
