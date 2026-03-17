package top.chengdongqing.wechat.features.contacts.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.network.model.FriendEvent
import top.chengdongqing.wechat.data.network.model.FriendProtocol
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest

interface FriendRequestRepository {

    val friendEvents: Flow<FriendEvent>

    /**
     * 获取所有好友申请
     */
    fun observeAllRequests(): Flow<List<FriendRequest>>

    /**
     * 获取待处理数量
     */
    fun getPendingCount(): Flow<Int>

    /**
     * 获取未读数量
     */
    fun observeUnreadCount(): Flow<Int>

    /**
     * 标记所有收到的申请为已读
     */
    suspend fun markAllIncomingAsRead()

    /**
     * 检查并标记过期的请求
     */
    suspend fun checkAndMarkExpired(expireDays: Int = 3)

    /**
     * 删除申请记录
     */
    suspend fun deleteRequest(requestId: String)

    /**
     * 发送好友申请
     */
    suspend fun sendFriendRequest(
        targetContact: Contact,
        greeting: String,
        remark: String? = null,
        note: String? = null
    ): Result<Unit>

    /**
     * 接受好友申请
     */
    suspend fun acceptFriendRequest(
        requestId: String,
        remark: String? = null,
        note: String? = null
    ): Result<Unit>

    /**
     * 处理收到的好友申请
     */
    suspend fun handleIncomingRequest(
        request: FriendProtocol.FriendRequest,
        avatarData: ByteArray?
    ): Result<Unit>

    /**
     * 处理申请响应
     */
    suspend fun handleRequestResponse(response: FriendProtocol.FriendResponse): Result<Unit>
}