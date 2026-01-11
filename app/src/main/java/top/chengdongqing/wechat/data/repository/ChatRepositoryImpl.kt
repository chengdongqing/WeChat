package top.chengdongqing.wechat.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.AppJson
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.local.MessageDao
import top.chengdongqing.wechat.data.local.MessageEntity
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MediaResource
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import top.chengdongqing.wechat.data.model.isSignal
import top.chengdongqing.wechat.data.network.P2pConnectionManager

class ChatRepositoryImpl(
    private val messageDao: MessageDao,
    private val connectionManager: P2pConnectionManager
) : ChatRepository {

    override val messages: Flow<List<MessageEntity>> = messageDao.getAllMessages()

    override suspend fun sendText(peer: P2PPeer, text: String): Result<MessageEntity> =
        withContext(
            Dispatchers.IO
        ) {
            val payload = ChatPayload.Text(content = text)
            val payloadJsonString =
                AppJson.instance.encodeToString(ChatPayload.serializer(), payload)

            // 先构造一个“发送中”的实体存入数据库，让 UI 先显示出来
            val messageId = randomUUID()
            val entity = MessageEntity(
                id = messageId,
                chatId = peer.id,
                senderId = "me", // 假设我的 ID 是固定的
                senderName = "MyName",
                payloadJson = payloadJsonString, // 这里简单处理，以后可以转 JSON
                msgType = "TEXT",
                timestamp = System.currentTimeMillis(),
                isFromMe = true,
                status = 0 // 发送中
            )
            messageDao.insertMessage(entity)

            // 调用底层网络
            val isNetworkSuccess = connectionManager.sendText(peer, text)

            println("-----isNetworkSuccess:$isNetworkSuccess,entity:$entity")

            if (isNetworkSuccess) {
                // 更新数据库状态为：成功 (1)
                messageDao.updateStatus(messageId, 1) // 1: 成功
                Result.success(entity.copy(status = 1))
            } else {
                // 更新数据库状态为：失败 (2)
                messageDao.updateStatus(messageId, 2) // 2: 失败
                Result.failure(Exception("网络发射失败"))
            }
        }

    override suspend fun sendImage(
        peer: P2PPeer,
        mediaResource: MediaResource
    ): Result<MessageEntity> =
        withContext(Dispatchers.IO) {
            // 2. 构造 Media Payload
            val payload = ChatPayload.Media(
                fileId = randomUUID(),
                fileName = mediaResource.fileName,
                mimeType = mediaResource.mimeType,
                size = mediaResource.size,
                localPath = mediaResource.file.absolutePath,
                thumbBase64 = mediaResource.thumbBase64
            )

            // 3. 构造消息实体并存库 (状态：发送中 0)
            val messageId = randomUUID()
            val entity = MessageEntity(
                id = messageId,
                chatId = peer.id,
                senderId = "me",
                senderName = "MyName",
                payloadJson = AppJson.instance.encodeToString(ChatPayload.serializer(), payload),
                msgType = "MEDIA",
                timestamp = System.currentTimeMillis(),
                isFromMe = true,
                status = 0, // 此时 UI 观察 Room 会立刻刷出带缩略图的消息
                progress = 0f
            )
            messageDao.insertMessage(entity)

            // 4. 调用底层网络传输二进制流
            var lastUpdate = 0L
            val isNetworkSuccess = connectionManager.sendMedia(
                peer = peer,
                payload = payload,
                file = mediaResource.file
            ) { progress ->
                // 可选：更新数据库中的进度字段以实现实时进度条
                val currentTime = System.currentTimeMillis()

                // 性能优化：每隔 200ms 或者进度跳跃超过 5% 才写一次数据库
                if (currentTime - lastUpdate > 200 || progress >= 1f) {
                    messageDao.updateProgress(messageId, progress)
                    lastUpdate = currentTime
                }
            }

            if (isNetworkSuccess) {
                // 更新数据库状态为：成功 (1)
                messageDao.updateStatus(messageId, 1) // 1: 成功
                Result.success(entity.copy(status = 1))
            } else {
                // 更新数据库状态为：失败 (2)
                messageDao.updateStatus(messageId, 2) // 2: 失败
                Result.failure(Exception("网络发射失败"))
            }
        }

    override suspend fun onMessageReceived(envelope: MessageEnvelope) {
        val payload = envelope.payload

        // 1. 拦截逻辑：如果是 WebRTC 信令或通话指令，直接跳过数据库存储
        if (payload.isSignal) {
            // 打印个日志方便调试，然后直接返回
            println("----收信: 检测到信令 ${payload::class.simpleName}，跳过数据库录入")
            return
        }

        // 2. 正常消息（文本或媒体）的保存逻辑
        val entity = MessageEntity(
            id = randomUUID(),
            chatId = envelope.senderId,
            senderId = envelope.senderId,
            senderName = envelope.senderName, // 后续可以从 peers.value 中根据 id 找 name
            payloadJson = AppJson.instance.encodeToString(envelope.payload),
            msgType = if (envelope.payload is ChatPayload.Media) "MEDIA" else "TEXT",
            timestamp = envelope.timestamp,
            isFromMe = false,
            status = 1 // 接收到的默认为成功
        )
        messageDao.insertMessage(entity)
    }
}