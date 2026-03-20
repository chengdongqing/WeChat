package top.chengdongqing.wechat.data.network.messaging

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.peerId
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.call.model.CallStatus
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

/**
 * 会话状态更新器
 */
@Singleton
class ChatSessionUpdater @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    private val chatSessionRepository: ChatSessionRepository,
    private val contactRepository: ContactRepository,
    profileRepository: ProfileRepository,
    private val activeSessionManager: ActiveSessionManager,
    @param:IoScope private val scope: CoroutineScope
) {
    private var currentProfile: UserProfile? = null

    init {
        scope.launch {
            profileRepository.observeProfile().collect {
                currentProfile = it
            }
        }
    }

    /**
     * 根据新消息更新对应会话
     */
    suspend fun update(message: MessageEntity, isSending: Boolean = false) {
        // 是否是和自己的会话
        val isSelfSession = message.receiverId == message.senderId

        // 是否需要更新未读计数
        val shouldIncrementUnread = when {
            message.isFromMe -> false // 我发的消息
            isSelfSession -> false // 发给我自己的
            activeSessionManager.isActive(message.sessionId) -> false // 当前正在和ta聊天
            message.isFinishedCall -> false // 是已接通的通话消息
            else -> true
        }

        // 只有文本才需要保存预览的文本
        val previewText = when (message.contentType) {
            MessageType.Text -> message.content
            else -> ""
        }
        val existing = chatSessionRepository.exists(message.sessionId)

        if (existing) {
            // 会话已存在：更新最新消息
            chatSessionDao.updateLastMessage(
                sessionId = message.sessionId,
                lastMessageId = message.id,
                lastMessage = previewText,
                lastMessageType = message.contentType,
                lastMessageTime = message.timestamp,
                lastMessageRecalled = false,
                lastMessageFromMe = message.isFromMe,
                isSending = isSending
            )
            // 累加未读数
            if (shouldIncrementUnread) {
                chatSessionDao.incrementUnreadCount(message.sessionId)
            }
        } else {
            // 创建新会话
            val (contactName, contactAvatar) = resolveContactInfo(message.peerId, isSelfSession)

            chatSessionDao.insert(
                ChatSessionEntity(
                    id = message.sessionId,
                    contactId = message.peerId,
                    contactName = contactName,
                    contactAvatar = contactAvatar,
                    lastMessageId = message.id,
                    lastMessage = previewText,
                    lastMessageType = message.contentType,
                    lastMessageTime = message.timestamp,
                    lastMessageFromMe = message.isFromMe,
                    isSending = isSending,
                    unreadCount = if (shouldIncrementUnread) 1 else 0
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
        contactId: String,
        isSelfSession: Boolean,
    ): Pair<String, String?> {
        return when {
            // 使用自己的个人资料
            isSelfSession -> {
                Pair(currentProfile?.nickname ?: "", currentProfile?.avatarPath)
            }

            // 去数据库查询联系人信息
            else -> {
                val contact = contactRepository.getContact(contactId)
                Pair(contact?.displayName ?: "", contact?.avatarPath)
            }
        }
    }
}

private val MessageEntity.isFinishedCall: Boolean
    get() = contentType.isCallMessage
            && CallStatus.valueOf(content) == CallStatus.Finished