package top.chengdongqing.wechat.features.chat.data.repository

import android.util.Log
import android.util.LruCache
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.util.deleteLocalFileBatch
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.features.chat.data.mapper.toDomain
import top.chengdongqing.wechat.features.chat.data.mapper.toEntity
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import javax.inject.Inject

class ChatSessionRepositoryImpl @Inject constructor(
    private val weDatabase: WeDatabase,
    private val chatSessionDao: ChatSessionDao,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageDao: MessageDao
) : ChatSessionRepository {

    // 会话缓存
    private val sessionCache = LruCache<String, ChatSession>(100)

    override fun observeAllSessions(): Flow<List<ChatSession>> {
        return chatSessionDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeSession(sessionId: String): Flow<ChatSession?> {
        val sessionFlow = chatSessionDao.observeById(sessionId)
        val onlineStatusFlow = connectionInfoDao.observeOnlineStatus(sessionId)

        return sessionFlow.combine(onlineStatusFlow) { entity, isOnline ->
            entity?.toDomain()?.copy(
                isOnline = isOnline ?: false
            )
        }
    }

    override suspend fun getSessionById(sessionId: String): ChatSession? {
        // 先从缓存拿
        synchronized(sessionCache) {
            sessionCache.get(sessionId)?.let { return it }
        }
        // 缓存没有，查库
        val session = chatSessionDao.getById(sessionId)?.toDomain()
        // 查到后回填缓存
        if (session != null) {
            synchronized(sessionCache) {
                sessionCache.put(sessionId, session)
            }
        }
        return session
    }

    override suspend fun isSessionMuted(sessionId: String): Boolean {
        return getSessionById(sessionId)?.isMuted.isTrue()
    }

    override suspend fun exists(sessionId: String): Boolean {
        return getSessionById(sessionId) != null
    }

    override suspend fun insertSession(session: ChatSession) {
        chatSessionDao.insert(session.toEntity())
    }

    override suspend fun clearUnreadCount(sessionId: String) {
        chatSessionDao.update(sessionId) { session ->
            session.copy(unreadCount = 0)
        }
        sessionCache.remove(sessionId)
    }

    override suspend fun markAsUnread(sessionId: String) {
        chatSessionDao.update(sessionId) { session ->
            session.copy(unreadCount = 1)
        }
        sessionCache.remove(sessionId)
    }

    override suspend fun updateDraft(sessionId: String, draft: String?) {
        chatSessionDao.update(sessionId) { session ->
            session.copy(draftMessage = draft)
        }
        sessionCache.remove(sessionId)
    }

    override suspend fun togglePin(sessionId: String, isPinned: Boolean) {
        chatSessionDao.update(sessionId) { session ->
            session.copy(isPinned = isPinned)
        }
        sessionCache.remove(sessionId)
    }

    override suspend fun toggleMute(sessionId: String, isMuted: Boolean) {
        chatSessionDao.update(sessionId) { session ->
            session.copy(isMuted = isMuted)
        }
        sessionCache.remove(sessionId)
    }

    override suspend fun toggleSpeaker(sessionId: String, isSpeakerOn: Boolean) {
        chatSessionDao.update(sessionId) { session ->
            session.copy(isSpeakerOn = isSpeakerOn)
        }
        sessionCache.remove(sessionId)
    }

    override suspend fun updateBackground(sessionId: String, backgroundPath: String?) {
        chatSessionDao.update(sessionId) { session ->
            session.copy(backgroundPath = backgroundPath)
        }
        sessionCache.remove(sessionId)
    }

    override suspend fun hideSession(sessionId: String) {
        chatSessionDao.update(sessionId) { session ->
            session.copy(isHidden = true)
        }
        sessionCache.remove(sessionId)
    }

    override suspend fun deleteSessionById(sessionId: String, shouldHide: Boolean) {
        // 查询当前会话所有的媒体文件，方便统一删除
        val paths = messageDao.getLocalPathsBySessionId(sessionId)

        // 删除会话，不真正删除这条记录，目的是保留 置顶/免到扰 等设置
        // 执行：清空消息+隐藏会话
        weDatabase.withTransaction {
            chatSessionDao.clearLastMessage(sessionId)
            if (shouldHide) {
                hideSession(sessionId)
            }
            messageDao.deleteBySessionId(sessionId)
        }

        // 删除媒体文件
        try {
            deleteLocalFileBatch(paths)
        } catch (e: Exception) {
            Log.e("DeleteSessionById", "删除文件失败", e)
        }

        // 从缓存清除
        sessionCache.remove(sessionId)
    }

    override fun observeTotalUnreadCount(): Flow<Int> {
        return chatSessionDao.observeAll().map { list ->
            list.sumOf {
                // 排除开启了免到扰的
                if (!it.isMuted) {
                    it.unreadCount
                } else {
                    0
                }
            }
        }
    }

    override suspend fun preload() {
        observeAllSessions().firstOrNull()
    }
}