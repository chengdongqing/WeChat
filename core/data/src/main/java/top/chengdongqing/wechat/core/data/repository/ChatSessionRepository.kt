package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.model.ChatSession

interface ChatSessionRepository {
    fun observeAllSessions(): Flow<List<ChatSession>>
    fun observeSession(sessionId: String): Flow<ChatSession?>
    suspend fun getSession(sessionId: String): ChatSession?
    suspend fun isSessionMuted(sessionId: String): Boolean
    suspend fun exists(sessionId: String): Boolean
    suspend fun createSession(session: ChatSession)
    suspend fun clearUnreadCount(sessionId: String)
    suspend fun markAsUnread(sessionId: String)
    suspend fun updateDraft(sessionId: String, draft: String?)
    suspend fun togglePin(sessionId: String, isPinned: Boolean)
    suspend fun toggleBottom(sessionId: String, isBottomed: Boolean)
    suspend fun toggleMute(sessionId: String, isMuted: Boolean)
    suspend fun updateBackground(sessionId: String, backgroundPath: String?)
    suspend fun setTemporary(sessionId: String, expiresAt: Long?)
    suspend fun cleanupExpiredTemporarySessions(now: Long = System.currentTimeMillis()): List<String>
    suspend fun hideSession(sessionId: String)
    suspend fun deleteSession(sessionId: String, shouldHide: Boolean = true)
    suspend fun deleteAllSessions()
    fun observeTotalUnreadCount(): Flow<Int>
    suspend fun preload()
}
