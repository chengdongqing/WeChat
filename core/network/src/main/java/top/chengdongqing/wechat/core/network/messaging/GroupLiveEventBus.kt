package top.chengdongqing.wechat.core.network.messaging

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.chengdongqing.wechat.core.common.util.randomUUID
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.crypto.PacketSigner
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketType
import top.chengdongqing.wechat.core.network.security.KeyStoreManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 群直播的临时事件通道。事件走群 Mesh 网络，但不写消息表、不更新会话预览。
 */
@Singleton
class GroupLiveEventBus @Inject constructor(
    private val groupDao: GroupDao,
    private val messageDao: MessageDao,
    private val profileRepository: ProfileRepository,
    private val transport: ChatTransportManager,
    private val packetSigner: PacketSigner,
    private val keyStoreManager: KeyStoreManager,
    private val json: Json
) {
    private val _events = MutableSharedFlow<ChatProtocol.GroupLiveEvent>(
        extraBufferCapacity = 128
    )
    val events = _events.asSharedFlow()

    suspend fun send(
        groupId: String,
        liveId: String,
        status: String,
        displayName: String,
        targetId: String? = null,
        payload: String? = null
    ): Result<Unit> = runCatching {
        val me = profileRepository.requireProfile()
        groupDao.getById(groupId) ?: error("群聊不存在")
        val unsigned = ChatProtocol.GroupLiveEvent(
            messageId = randomUUID(),
            senderId = me.id,
            signature = "",
            groupId = groupId,
            liveId = liveId,
            status = status,
            displayName = displayName,
            targetId = targetId,
            payload = payload
        )
        val event = unsigned.copy(
            signature = packetSigner.sign(unsigned, keyStoreManager.getPrivateKey())
        )
        val packet = Packet(
            PacketType.TEXT,
            json.encodeToString<ChatProtocol>(event).toByteArray(Charsets.UTF_8)
        )
        val targets = groupDao.getMembers(groupId)
            .map { it.userId }
            .filter { it != me.id && (targetId == null || it == targetId) }
        val results = targets.map { transport.send(it, packet) }
        if (targets.isNotEmpty() && results.none { it.isSuccess }) {
            throw results.firstNotNullOfOrNull { it.exceptionOrNull() }
                ?: IllegalStateException("没有可达的群成员")
        }
    }

    internal suspend fun receive(event: ChatProtocol.GroupLiveEvent) {
        val group = groupDao.getById(event.groupId) ?: return
        if (groupDao.getMembers(group.id).none { it.userId == event.senderId }) return
        if (event.status == "ended") updateEndedCard(event)
        _events.emit(event)
    }

    private suspend fun updateEndedCard(event: ChatProtocol.GroupLiveEvent) {
        messageDao.getBySessionAndType(event.groupId, MessageType.Live).forEach { message ->
            val content = runCatching { json.parseToJsonElement(message.content).jsonObject }
                .getOrNull() ?: return@forEach
            if (content["liveId"]?.jsonPrimitive?.content != event.liveId) return@forEach
            val updated = content.toMutableMap().apply {
                put("status", kotlinx.serialization.json.JsonPrimitive("ended"))
            }
            messageDao.update(message.copy(content = JsonObject(updated).toString()))
        }
    }
}
