package top.chengdongqing.wechat.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey
    val id: String,              // 请求ID

    // 基本信息
    val peerUserId: String,             // 对方的ID
    val peerNickname: String,           // 对方的昵称
    val peerAvatarPath: String?,        // 对方的头像路径

    // 申请内容
    val greetingMessage: String,        // 打招呼内容
    val remark: String? = null,         // 备注
    val tags: String? = null,           // 标签
    val note: String? = null,           // 备忘

    // 状态
    val status: RequestStatus,          // 状态
    val direction: RequestDirection,    // 方向（发出/收到）

    // 新增：是否已读
    val isRead: Boolean = false,

    // 时间
    val createAt: Long,                // 创建时间
    val updatedAt: Long                 // 更新时间
)

enum class RequestStatus {
    Pending,    // 待处理
    Accepted,   // 已接受
    Rejected,   // 已拒绝
    Expired     // 已过期
}

enum class RequestDirection {
    Outgoing,   // 发出的
    Incoming;   // 收到的

    val isOutgoing: Boolean get() = this == Outgoing
}