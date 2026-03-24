package top.chengdongqing.wechat.feature.chat.data.repository

import android.util.LruCache
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.common.file.PrivateFileManager
import top.chengdongqing.wechat.core.common.util.getOrPutAsync
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.model.ChatSession
import top.chengdongqing.wechat.core.network.messaging.ChunkStorageManager
import top.chengdongqing.wechat.core.network.session.FileReferenceManager
import top.chengdongqing.wechat.feature.chat.data.mapper.toDomain
import top.chengdongqing.wechat.feature.chat.data.mapper.toEntity
import javax.inject.Inject

class ChatSessionRepositoryImpl @Inject constructor(
    private val database: WeDatabase,
    private val chatSessionDao: ChatSessionDao,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageDao: MessageDao,
    private val fileReferenceManager: FileReferenceManager,
    private val chunkStorageManager: ChunkStorageManager,
    private val privateFileManager: PrivateFileManager
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

    override suspend fun getSession(sessionId: String): ChatSession? {
        return sessionCache.getOrPutAsync(sessionId) {
            chatSessionDao.getById(sessionId)?.toDomain()
        }
    }

    override suspend fun isSessionMuted(sessionId: String): Boolean {
        return getSession(sessionId)?.isMuted.isTrue()
    }

    override suspend fun exists(sessionId: String): Boolean {
        return getSession(sessionId) != null
    }

    override suspend fun createSession(session: ChatSession) {
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

    override suspend fun deleteSession(sessionId: String, shouldHide: Boolean) {
        // 查询当前会话所有的媒体文件，方便统一删除
        val localPaths = messageDao.getLocalPathsBySessionId(sessionId)
        val messageIds = messageDao.getTransferRelatedIdsBySessionId(sessionId)

        // 删除会话，不真正删除这条记录，目的是保留 置顶/免到扰 等设置
        // 执行：清空消息+隐藏会话
        database.withTransaction {
            chatSessionDao.clearLastMessage(sessionId)
            if (shouldHide) {
                hideSession(sessionId)
            }
            messageDao.deleteBySessionId(sessionId)
        }

        // 清理可能存在的本地文件
        val toDelete = fileReferenceManager.releaseAll(localPaths)
        privateFileManager.deleteFiles(toDelete)

        // 清理可能存在的文件分片
        chunkStorageManager.cleanupBatch(messageIds)

        // 从缓存清除
        sessionCache.remove(sessionId)
    }

    override suspend fun deleteAllSessions() {
        // 查询所有会话的媒体文件路径
        val localPaths = messageDao.getAllLocalPaths()

        // 清空所有消息 + 隐藏所有会话（保留置顶/免打扰等设置）
        database.withTransaction {
            chatSessionDao.clearAll()
            messageDao.deleteAll()
        }

        // 清理可能存在的本地文件
        val toDelete = fileReferenceManager.releaseAll(localPaths)
        privateFileManager.deleteFiles(toDelete)

        // 清理可能存在的文件分片
        chunkStorageManager.clearAllTransfers()

        // 清空会话缓存
        sessionCache.evictAll()
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