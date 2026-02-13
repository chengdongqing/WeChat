package top.chengdongqing.wechat.data.network.messaging

import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.toPreviewText
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

@Singleton
class ChatSessionUpdater @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    private val contactDao: ContactDao,
    private val profileRepository: ProfileRepository,
    private val activeSessionManager: ActiveSessionManager
) {

    suspend fun update(entity: MessageEntity) {
        val isSelfSession = entity.receiverId == entity.senderId
        val isCurrentlyViewing = activeSessionManager.isActive(entity.sessionId)
        val unreadIncrement = if (entity.isFromMe || isSelfSession || isCurrentlyViewing) 0 else 1
        val lastMessageText = entity.contentType.toPreviewText(entity.content)

        val existing = chatSessionDao.getById(entity.sessionId)
        if (existing != null) {
            chatSessionDao.updateLastMessage(
                entity.sessionId,
                lastMessageText,
                entity.contentType,
                entity.timestamp
            )
            if (unreadIncrement > 0) {
                chatSessionDao.incrementUnreadCount(entity.sessionId)
            }
        } else {
            val (contactName, contactAvatar) = resolveContactInfo(entity, isSelfSession)
            chatSessionDao.insert(
                ChatSessionEntity(
                    sessionId = entity.sessionId,
                    contactId = entity.senderId,
                    contactName = contactName,
                    contactAvatar = contactAvatar,
                    lastMessage = lastMessageText,
                    lastMessageType = entity.contentType,
                    lastMessageTime = entity.timestamp,
                    unreadCount = unreadIncrement,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun resolveContactInfo(
        entity: MessageEntity,
        isSelfSession: Boolean
    ): Pair<String, String?> {
        return if (isSelfSession) {
            val profile = profileRepository.getCurrentProfileOnce()
            Pair(profile?.nickname ?: "", profile?.avatarPath)
        } else {
            val contact = contactDao.getById(entity.senderId)
            Pair(
                contact?.remarkName?.takeIf { it.isNotBlank() } ?: contact?.nickname ?: "",
                contact?.avatarPath
            )
        }
    }
}