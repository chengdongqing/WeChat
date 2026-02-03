package top.chengdongqing.wechat2.data.repository.chat

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat2.data.local_1.MessageEntity
import top.chengdongqing.wechat2.data.model_1.MessageEnvelope
import top.chengdongqing.wechat2.data.model_1.P2PPeer
import java.io.File

interface ChatRepository {
    // 观察消息流
    val messages: Flow<List<MessageEntity>>

    // 发送消息
    suspend fun sendText(peer: P2PPeer, envelope: MessageEnvelope): Result<MessageEntity>

    // 发送图片
    suspend fun sendMedia(
        peer: P2PPeer,
        envelope: MessageEnvelope,
        file: File
    ): Result<MessageEntity>
}