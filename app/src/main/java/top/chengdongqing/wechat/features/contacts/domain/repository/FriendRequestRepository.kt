package top.chengdongqing.wechat.features.contacts.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendProfileResponse
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequestResponse
import top.chengdongqing.wechat.features.contacts.domain.model.IncomingFriendRequest

interface FriendRequestRepository {

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
    suspend fun handleIncomingRequest(request: IncomingFriendRequest): Result<Unit>

    /**
     * 处理申请响应
     */
    suspend fun handleRequestResponse(response: FriendRequestResponse): Result<Unit>

    /**
     * 处理自动添加（对方没有把我删掉，我主动加回）
     */
    suspend fun handleAutoAdd(response: FriendProfileResponse): Result<Unit>
}