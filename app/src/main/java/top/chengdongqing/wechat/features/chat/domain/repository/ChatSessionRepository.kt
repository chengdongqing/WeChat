package top.chengdongqing.wechat.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession

interface ChatSessionRepository {

    /** 监听所有会话 */
    fun observeAllSessions(): Flow<List<ChatSession>>

    /** 监听单个会话 */
    fun observeSession(sessionId: String): Flow<ChatSession?>

    /** 获取单个会话 */
    suspend fun getSession(sessionId: String): ChatSession?

    /** 创建或更新会话 */
    suspend fun upsertSession(session: ChatSession)

    /** 清空未读数 */
    suspend fun clearUnreadCount(sessionId: String)

    /** 标记为未读 */
    suspend fun markAsUnread(sessionId: String)

    /** 保存草稿 */
    suspend fun saveDraft(sessionId: String, draft: String?)

    /** 置顶/取消置顶 */
    suspend fun togglePin(sessionId: String, isPinned: Boolean)

    /** 隐藏会话 */
    suspend fun hideSession(sessionId: String)

    /** 删除会话 */
    suspend fun deleteSession(sessionId: String)

    /** 总未读数 */
    fun observeTotalUnreadCount(): Flow<Int>
}