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

/**
 * 会话状态更新器
 *
 * 每条消息入库后调用，负责维护 ChatSession 的最新消息预览和未读计数。
 * 会话不存在时自动创建，联系人信息从 [ContactDao] / [ProfileRepository] 实时解析。
 */
@Singleton
class ChatSessionUpdater @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    private val contactDao: ContactDao,
    private val profileRepository: ProfileRepository,
    private val activeSessionManager: ActiveSessionManager
) {
    /**
     * 根据新消息更新对应会话
     *
     * 未读计数规则：自己发的、给自己发的（自我会话）、当前正在查看的，均不计未读。
     */
    suspend fun update(entity: MessageEntity) {
        val isSelfSession = entity.receiverId == entity.senderId
        val isCurrentlyViewing = activeSessionManager.isActive(entity.sessionId)
        val unreadIncrement = if (entity.isFromMe || isSelfSession || isCurrentlyViewing) 0 else 1
        val lastMessageText = entity.contentType.toPreviewText(entity.content)

        val existing = chatSessionDao.getById(entity.sessionId)
        if (existing != null) {
            // 会话已存在：更新最新消息，按需累加未读数
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
            // 会话不存在：创建新会话，联系人信息需要实时解析
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

    /**
     * 解析联系人显示信息
     *
     * 自我会话：取自己的昵称和头像
     * 普通会话：优先用备注名，无备注则用昵称；发件人是自己时取收件人信息，反之取发件人信息
     */
    private suspend fun resolveContactInfo(
        entity: MessageEntity,
        isSelfSession: Boolean
    ): Pair<String, String?> {
        val profile = profileRepository.getCurrentProfileOnce()

        return if (isSelfSession) {
            Pair(profile?.nickname ?: "", profile?.avatarPath)
        } else {
            val contactId =
                if (entity.senderId == profile?.id) entity.receiverId else entity.senderId
            val contact = contactDao.getById(contactId)
            Pair(
                contact?.remarkName?.takeIf { it.isNotBlank() } ?: contact?.nickname ?: "",
                contact?.avatarPath
            )
        }
    }
}