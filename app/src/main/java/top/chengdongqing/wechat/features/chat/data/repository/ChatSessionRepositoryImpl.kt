package top.chengdongqing.wechat.features.chat.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.features.chat.data.mapper.toDomain
import top.chengdongqing.wechat.features.chat.data.mapper.toEntity
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import javax.inject.Inject

class ChatSessionRepositoryImpl @Inject constructor(
    private val weDatabase: WeDatabase,
    private val chatSessionDao: ChatSessionDao,
    private val messageDao: MessageDao
) : ChatSessionRepository {

    override fun observeAllSessions(): Flow<List<ChatSession>> {
        return chatSessionDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeSession(sessionId: String): Flow<ChatSession?> {
        return chatSessionDao.observeById(sessionId).map { it?.toDomain() }
    }

    override suspend fun getSession(sessionId: String): ChatSession? {
        return chatSessionDao.getById(sessionId)?.toDomain()
    }

    override suspend fun exists(sessionId: String): Boolean {
        return chatSessionDao.exists(sessionId)
    }

    override suspend fun insertSession(session: ChatSession) {
        chatSessionDao.insert(session.toEntity())
    }

    override suspend fun clearUnreadCount(sessionId: String) {
        chatSessionDao.clearUnreadCount(sessionId)
    }

    override suspend fun markAsUnread(sessionId: String) {
        chatSessionDao.markAsUnread(sessionId)
    }

    override suspend fun saveDraft(sessionId: String, draft: String?) {
        chatSessionDao.updateDraft(sessionId, draft)
    }

    override suspend fun togglePin(sessionId: String, isPinned: Boolean) {
        chatSessionDao.updatePin(sessionId, isPinned)
    }

    override suspend fun toggleMute(sessionId: String, isMuted: Boolean) {
        chatSessionDao.updateMute(sessionId, isMuted)
    }

    override suspend fun hideSession(sessionId: String) {
        chatSessionDao.hideSession(sessionId)
    }

    override suspend fun deleteSession(sessionId: String, shouldHide: Boolean) {
        // 删除会话，不真正删除这条记录，目的是这里还保存了是否置顶/免到扰等设置
        // 只需要清空消息，然后隐藏会话即可
        weDatabase.withTransaction {
            chatSessionDao.clearLastMessage(sessionId)
            if (shouldHide) {
                chatSessionDao.hideSession(sessionId)
            }
            messageDao.deleteBySession(sessionId)
        }
    }

    override fun observeTotalUnreadCount(): Flow<Int> {
        return chatSessionDao.observeAll().map { list ->
            list.sumOf { it.unreadCount }
        }
    }

    override suspend fun preload() {
        observeAllSessions().firstOrNull()
    }
}