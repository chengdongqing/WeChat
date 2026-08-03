package top.chengdongqing.wechat.core.network.messaging

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.repository.ChatSettingsRepository
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.crypto.PacketSigner
import top.chengdongqing.wechat.core.network.http.AvatarServer
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketType
import top.chengdongqing.wechat.core.network.security.KeyStoreManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemporaryChatCoordinator @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    private val contactRepository: ContactRepository,
    private val chatSettingsRepository: ChatSettingsRepository,
    private val profileRepository: ProfileRepository,
    private val transport: ChatTransportManager,
    private val packetSigner: PacketSigner,
    private val keyStoreManager: KeyStoreManager,
    private val avatarServer: AvatarServer,
    private val temporaryAvatarStore: TemporaryAvatarStore,
    private val json: Json
) {
    suspend fun invite(receiverId: String, expiresAt: Long): Result<Unit> = runCatching {
        val profile = profileRepository.requireProfile()
        require(receiverId != profile.id) { "不能邀请自己" }
        require(
            expiresAt in (System.currentTimeMillis() + 1)..
                    (System.currentTimeMillis() + MAX_DURATION_MS)
        ) { "临时聊天有效期无效" }
        val publicKey = requireNotNull(profile.publicKey) { "本机身份公钥不可用" }
        val unsigned = ChatProtocol.TemporaryChatInvite(
            messageId = UUID.randomUUID().toString(),
            senderId = profile.id,
            receiverId = receiverId,
            nickname = profile.nickname,
            avatarUrl = avatarServer.avatarUrl,
            publicKey = publicKey,
            expiresAt = expiresAt,
            signature = ""
        )
        val signed = unsigned.copy(
            signature = packetSigner.sign(unsigned, keyStoreManager.getPrivateKey())
        )
        transport.send(
            receiverId,
            Packet(
                PacketType.TEXT,
                json.encodeToString<ChatProtocol>(signed).toByteArray(Charsets.UTF_8)
            )
        ).getOrThrow()
    }

    suspend fun receive(invite: ChatProtocol.TemporaryChatInvite) {
        if (!chatSettingsRepository.temporaryChatEnabled.first()) return
        val now = System.currentTimeMillis()
        if (invite.expiresAt <= now) return
        val existing = chatSessionDao.getById(invite.senderId)
        // 正式好友会话不被陌生邀请降级；非好友留下的隐藏/旧占位会话可以升级。
        if (existing != null && !existing.isTemporary && contactRepository.exists(invite.senderId)) {
            return
        }
        val localAvatar = runCatching {
            temporaryAvatarStore.persist(invite.senderId, invite.avatarUrl)
        }.getOrNull()

        if (existing == null) {
            chatSessionDao.insert(
                ChatSessionEntity(
                    id = invite.senderId,
                    contactId = invite.senderId,
                    contactName = invite.nickname.take(MAX_NICKNAME_LENGTH),
                    contactAvatar = localAvatar ?: invite.avatarUrl,
                    lastMessageId = invite.messageId,
                    lastMessage = INVITATION_PREVIEW,
                    lastMessageType = MessageType.Text,
                    lastMessageTime = invite.timestamp,
                    lastMessageFromMe = false,
                    unreadCount = 1,
                    isTemporary = true,
                    expiresAt = minOf(invite.expiresAt, now + MAX_DURATION_MS),
                    temporaryPeerPublicKey = invite.publicKey
                )
            )
        } else {
            chatSessionDao.update(
                existing.copy(
                    contactName = invite.nickname.take(MAX_NICKNAME_LENGTH),
                    contactAvatar = localAvatar ?: invite.avatarUrl ?: existing.contactAvatar,
                    lastMessageId = invite.messageId,
                    lastMessage = INVITATION_PREVIEW,
                    lastMessageType = MessageType.Text,
                    lastMessageTime = invite.timestamp,
                    lastMessageFromMe = false,
                    isHidden = false,
                    isTemporary = true,
                    expiresAt = minOf(invite.expiresAt, now + MAX_DURATION_MS),
                    temporaryPeerPublicKey = invite.publicKey,
                    unreadCount = maxOf(existing.unreadCount, 1)
                )
            )
        }
    }

    companion object {
        const val DEFAULT_DURATION_MS = 24 * 60 * 60 * 1000L
        const val MAX_DURATION_MS = DEFAULT_DURATION_MS
        private const val MAX_NICKNAME_LENGTH = 64
        private const val INVITATION_PREVIEW = "邀请你进行临时聊天"
    }
}
