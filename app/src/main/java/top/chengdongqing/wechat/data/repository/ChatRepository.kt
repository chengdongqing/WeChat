package top.chengdongqing.wechat.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.local.MessageEntity
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
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