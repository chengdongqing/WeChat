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

    /** 是否存在会话 */
    suspend fun exists(sessionId: String): Boolean

    /** 创建会话 */
    suspend fun insertSession(session: ChatSession)

    /** 清空未读数 */
    suspend fun clearUnreadCount(sessionId: String)

    /** 标记为未读 */
    suspend fun markAsUnread(sessionId: String)

    /** 保存草稿 */
    suspend fun updateDraft(sessionId: String, draft: String?)

    /** 置顶/取消置顶 */
    suspend fun togglePin(sessionId: String, isPinned: Boolean)

    /** 免到扰/取消免到扰 */
    suspend fun toggleMute(sessionId: String, isMuted: Boolean)

    /** 修改聊天背景 */
    suspend fun updateBackground(sessionId: String, backgroundPath: String?)

    /** 隐藏会话 */
    suspend fun hideSession(sessionId: String)

    /** 删除会话 */
    suspend fun deleteSession(sessionId: String, shouldHide: Boolean = true)

    /** 总未读数 */
    fun observeTotalUnreadCount(): Flow<Int>

    /** 数据库预热 */
    suspend fun preload()
}