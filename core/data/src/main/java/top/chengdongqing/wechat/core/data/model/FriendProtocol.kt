package top.chengdongqing.wechat.core.data.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.core.model.ContactAddSource

@Serializable
sealed class FriendProtocol {

    @Serializable
    data class FriendRequest(
        val requestId: String,
        val userId: String,
        val nickname: String,
        val publicKey: String,
        val greeting: String,
        val source: ContactAddSource?,
        val timestamp: Long
    ) : FriendProtocol()

    @Serializable
    data class FriendResponse(
        val requestId: String,
        val result: FriendRequestResult,
        val timestamp: Long
    ) : FriendProtocol()
}
