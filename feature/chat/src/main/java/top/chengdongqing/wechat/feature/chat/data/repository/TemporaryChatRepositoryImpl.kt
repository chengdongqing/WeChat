package top.chengdongqing.wechat.feature.chat.data.repository

import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.TemporaryChatRepository
import top.chengdongqing.wechat.core.model.ChatSession
import top.chengdongqing.wechat.core.network.messaging.TemporaryAvatarStore
import top.chengdongqing.wechat.core.network.messaging.TemporaryChatCoordinator
import javax.inject.Inject

class TemporaryChatRepositoryImpl @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository,
    private val coordinator: TemporaryChatCoordinator,
    private val temporaryAvatarStore: TemporaryAvatarStore
) : TemporaryChatRepository {
    override suspend fun start(
        peerId: String,
        peerName: String,
        peerAvatar: String?,
        peerPublicKey: String
    ): Result<Long> = runCatching {
        require(peerPublicKey.isNotBlank()) { "对方身份公钥不可用" }
        val expiresAt = System.currentTimeMillis() + TemporaryChatCoordinator.DEFAULT_DURATION_MS
        coordinator.invite(peerId, expiresAt).getOrThrow()
        val localAvatar = runCatching {
            temporaryAvatarStore.persist(peerId, peerAvatar)
        }.getOrNull()
        chatSessionRepository.createSession(
            ChatSession(
                id = peerId,
                contactId = peerId,
                contactName = peerName,
                contactAvatar = localAvatar ?: peerAvatar,
                isTemporary = true,
                expiresAt = expiresAt,
                temporaryPeerPublicKey = peerPublicKey
            )
        )
        expiresAt
    }
}
