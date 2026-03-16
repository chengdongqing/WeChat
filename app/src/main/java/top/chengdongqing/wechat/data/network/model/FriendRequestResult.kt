package top.chengdongqing.wechat.data.network.model

import kotlinx.serialization.Serializable

@Serializable
enum class FriendRequestResult {
    /**
     * 已接受
     */
    Accepted,

    /**
     * 已拒绝
     */
    Rejected
}