package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.data.model.FriendEvent
import top.chengdongqing.wechat.core.data.model.FriendProtocol
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.FriendRequest

interface FriendRequestRepository {
    val friendEvents: Flow<FriendEvent>
    fun observeAllRequests(): Flow<List<FriendRequest>>
    fun getPendingCount(): Flow<Int>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markAllIncomingAsRead()
    suspend fun checkAndMarkExpired(expireDays: Int = 3)
    suspend fun deleteRequest(requestId: String)
    suspend fun sendFriendRequest(
        targetContact: Contact,
        greeting: String,
        remark: String? = null,
        note: String? = null
    ): Result<Unit>
    suspend fun acceptFriendRequest(
        requestId: String,
        remark: String? = null,
        note: String? = null
    ): Result<Unit>
    suspend fun handleIncomingRequest(
        request: FriendProtocol.FriendRequest,
        avatarData: ByteArray?
    ): Result<Unit>
    suspend fun handleRequestResponse(response: FriendProtocol.FriendResponse): Result<Unit>
}
