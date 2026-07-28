package top.chengdongqing.wechat.core.network.messaging

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketType

/**
 * 局域网群聊的受控 Mesh 转发器。
 *
 * 仅向本机已经直连的群成员转发，messageId 去重，TTL 与 route 同时限制环路。
 * 缓存有上限，避免常驻服务运行数月后无限增长。
 */
@Singleton
class MeshGroupRouter @Inject constructor(
    private val groupDao: GroupDao,
    private val transport: ChatTransportManager,
    private val profileRepository: ProfileRepository,
    private val json: Json
) {
    private val lock = Mutex()
    private val seen = LinkedHashMap<String, Long>()

    suspend fun relay(message: ChatProtocol.GroupTextMessage, receivedFrom: String? = null) {
        if (message.ttl <= 0 || !markSeen(message.messageId)) return
        val me = profileRepository.requireUserId()
        val group = groupDao.getById(message.groupId) ?: return
        if (!group.meshEnabled) return

        val blocked = (message.route + message.senderId + me + listOfNotNull(receivedFrom)).toSet()
        val forwarded = message.copy(ttl = message.ttl - 1, route = message.route + me)
        val packet = Packet(
            PacketType.TEXT,
            json.encodeToString<ChatProtocol>(forwarded).toByteArray(Charsets.UTF_8)
        )
        groupDao.getMembers(message.groupId)
            .asSequence()
            .map { it.userId }
            .filterNot(blocked::contains)
            .filter(transport::isConnected)
            .forEach { transport.send(it, packet) }
    }

    private suspend fun markSeen(messageId: String): Boolean = lock.withLock {
        if (seen.containsKey(messageId)) return false
        seen[messageId] = System.currentTimeMillis()
        while (seen.size > MAX_SEEN_MESSAGES) seen.remove(seen.keys.first())
        true
    }

    private companion object {
        const val MAX_SEEN_MESSAGES = 4096
    }
}
