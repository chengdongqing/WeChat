package top.chengdongqing.wechat.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.local.MessageEntity
import top.chengdongqing.wechat.data.model.MediaResource
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer

interface ChatRepository {
    // 观察消息流
    val messages: Flow<List<MessageEntity>>

    // 发送消息
    suspend fun sendText(peer: P2PPeer, text: String): Result<MessageEntity>

    // 发送图片
    suspend fun sendImage(peer: P2PPeer, mediaResource: MediaResource): Result<MessageEntity>

    // 接收消息 (供网络层回调)
    suspend fun onMessageReceived(envelope: MessageEnvelope)
}