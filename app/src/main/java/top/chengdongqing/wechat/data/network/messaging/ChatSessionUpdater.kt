package top.chengdongqing.wechat.data.network.messaging

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.toPreviewText
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.call.domain.model.CallStatus
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

/**
 * 会话状态更新器
 *
 * 每条消息入库后调用，负责维护 ChatSession 的最新消息预览和未读计数。
 * 会话不存在时自动创建，联系人信息从 [ContactDao] / [ProfileRepository] 获取。
 */
@Singleton
class ChatSessionUpdater @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    private val contactRepository: ContactRepository,
    profileRepository: ProfileRepository,
    private val activeSessionManager: ActiveSessionManager,
    @param:IoScope private val scope: CoroutineScope
) {
    private var currentProfile: UserProfile? = null

    init {
        scope.launch {
            profileRepository.getCurrentProfile().collect {
                currentProfile = it
            }
        }
    }

    /**
     * 根据新消息更新对应会话
     *
     * 未读计数规则：自己发的、给自己发的（自我会话）、当前正在查看的，均不计未读。
     */
    suspend fun update(entity: MessageEntity) {
        // 是否是和自己的会话
        val isSelfSession = entity.receiverId == entity.senderId

        // 是否需要更新未读计数
        val shouldIncrementUnread = when {
            entity.isFromMe -> false // 我发的消息
            isSelfSession -> false // 发给我自己的
            activeSessionManager.isActive(entity.sessionId) -> false // 当前正在和ta聊天
            entity.isFinishedCall -> false // 是已接通的通话消息
            else -> true
        }

        val lastMessageText = entity.contentType.toPreviewText(entity.content)
        val existing = chatSessionDao.exists(entity.sessionId)

        if (existing) {
            // 会话已存在：更新最新消息
            chatSessionDao.updateLastMessage(
                sessionId = entity.sessionId,
                lastMessage = lastMessageText,
                lastMessageType = entity.contentType,
                timestamp = entity.timestamp
            )
            // 累加未读数
            if (shouldIncrementUnread) {
                chatSessionDao.incrementUnreadCount(entity.sessionId)
            }
        } else {
            // 创建新会话
            val (contactName, contactAvatar) = resolveContactInfo(entity, isSelfSession)
            val now = System.currentTimeMillis()

            chatSessionDao.insert(
                ChatSessionEntity(
                    sessionId = entity.sessionId,
                    contactId = entity.senderId,
                    contactName = contactName,
                    contactAvatar = contactAvatar,
                    lastMessage = lastMessageText,
                    lastMessageType = entity.contentType,
                    lastMessageTime = entity.timestamp,
                    unreadCount = if (shouldIncrementUnread) 1 else 0,
                    createdAt = now,
                    updatedAt = now
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
        isSelfSession: Boolean,
    ): Pair<String, String?> {
        return when {
            // 使用自己的个人资料
            isSelfSession -> {
                Pair(currentProfile?.nickname ?: "", currentProfile?.avatarPath)
            }

            // 去数据库查询联系人信息
            else -> {
                val contactId = if (entity.senderId == currentProfile?.id) {
                    entity.receiverId
                } else {
                    entity.senderId
                }
                val contact = contactRepository.getContactById(contactId)

                Pair(contact?.displayName ?: "", contact?.avatarPath)
            }
        }
    }
}

private val MessageEntity.isFinishedCall: Boolean
    get() = contentType.isCallMessage
            && CallStatus.valueOf(content) == CallStatus.Finished