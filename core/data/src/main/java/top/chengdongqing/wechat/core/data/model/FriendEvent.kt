package top.chengdongqing.wechat.core.data.model

sealed class FriendEvent {
    data class FriendRequest(val nickname: String) : FriendEvent()
    data class Added(val nickname: String, val contactId: String) : FriendEvent()
}
