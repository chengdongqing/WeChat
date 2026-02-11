package top.chengdongqing.wechat.features.contacts.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendProfileResponse
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequestResponse
import top.chengdongqing.wechat.features.contacts.domain.model.IncomingFriendRequest

interface FriendRequestRepository {

    // ==================== 查询 ====================

    /**
     * 获取所有好友申请
     */
    fun getRequests(): Flow<List<FriendRequest>>

    /**
     * 获取待处理数量
     */
    fun getPendingCount(): Flow<Int>

    /**
     * 获取未读数量
     */
    fun getUnreadCount(): Flow<Int>

    // ==================== 操作 ====================

    /**
     * 标记所有收到的申请为已读
     */
    suspend fun markAllIncomingAsRead()

    /**
     * 删除申请记录
     */
    suspend fun delete(requestId: String)

    /**
     * 发送好友申请
     */
    suspend fun sendFriendRequest(
        targetContact: Contact,
        greetingMessage: String,
        remark: String? = null,
        tags: List<String>? = null,
        note: String? = null
    ): Result<Unit>

    /**
     * 接受好友申请
     */
    suspend fun acceptFriendRequest(
        requestId: String,
        remark: String? = null,
        tags: List<String>? = null,
        note: String? = null
    ): Result<Unit>

    /**
     * 拒绝好友申请
     */
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>

    // ==================== 内部使用（可选暴露） ====================

    /**
     * 处理收到的好友申请（由 P2PService 调用）
     */
    suspend fun handleIncomingRequest(request: IncomingFriendRequest)

    /**
     * 处理申请响应（由 P2PService 调用）
     */
    suspend fun handleRequestResponse(response: FriendRequestResponse)

    /**
     * 处理自动添加回复（由 P2PService 调用）
     */
    suspend fun handleAutoAddResponse(response: FriendProfileResponse)

    /**
     * 处理完整资料响应（由 P2PService 调用）
     */
    suspend fun handleFullProfileResponse(response: FriendProfileResponse)
}