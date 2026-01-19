package top.chengdongqing.wechat.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.data.local.MessageDao
import top.chengdongqing.wechat.data.local.MessageEntity
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import top.chengdongqing.wechat.data.network.P2pConnectionManager
import java.io.File

class ChatRepositoryImpl(
    private val messageDao: MessageDao,
    private val connectionManager: P2pConnectionManager
) : ChatRepository {

    override val messages: Flow<List<MessageEntity>> = messageDao.getAllMessages()

    override suspend fun sendText(peer: P2PPeer, envelope: MessageEnvelope): Result<MessageEntity> {
        // 保存到数据库
        val messageId = randomUUID()
        val entity = MessageEntity(
            id = messageId,
            chatId = peer.id,
            senderId = envelope.senderId,
            senderName = envelope.senderName,
            payload = envelope.payload,
            msgType = "TEXT",
            isFromMe = true,
            status = 0 // 发送中
        )
        messageDao.insertMessage(entity)

        // 调用底层网络
        val isNetworkSuccess = connectionManager.sendText(peer, envelope)

        if (isNetworkSuccess) {
            // 更新数据库状态为：成功 (1)
            messageDao.updateStatus(messageId, 1) // 1: 成功
            return Result.success(entity.copy(status = 1))
        } else {
            // 更新数据库状态为：失败 (2)
            messageDao.updateStatus(messageId, 2) // 2: 失败
            return Result.failure(Exception("网络发射失败"))
        }
    }

    override suspend fun sendMedia(
        peer: P2PPeer,
        envelope: MessageEnvelope,
        file: File
    ): Result<MessageEntity> {
        // 保存到数据库
        val messageId = randomUUID()
        val entity = MessageEntity(
            id = messageId,
            chatId = peer.id,
            senderId = envelope.senderId,
            senderName = envelope.senderName,
            payload = envelope.payload,
            msgType = "MEDIA",
            isFromMe = true,
            status = 0,
            progress = 0f
        )
        messageDao.insertMessage(entity)

        // 调用底层网络
        var lastUpdate = 0L
        val isNetworkSuccess = connectionManager.sendMedia(
            peer = peer,
            envelope = envelope,
            file = file
        ) { progress ->
            // 每隔 200ms 更新一次数据库
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdate > 200 || progress >= 1f) {
                messageDao.updateProgress(messageId, progress)
                lastUpdate = currentTime
            }
        }

        if (isNetworkSuccess) {
            // 更新数据库状态为：成功 (1)
            messageDao.updateStatus(messageId, 1) // 1: 成功
            return Result.success(entity.copy(status = 1))
        } else {
            // 更新数据库状态为：失败 (2)
            messageDao.updateStatus(messageId, 2) // 2: 失败
            return Result.failure(Exception("网络发射失败"))
        }
    }
}