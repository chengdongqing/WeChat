package top.chengdongqing.wechat.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey
    val requestId: String,              // 请求ID

    // 基本信息
    val fromUserId: String,             // 发起人ID
    val fromNickname: String,           // 发起人昵称
    val fromAvatarPath: String?,        // 发起人头像路径

    val toUserId: String,               // 接收人ID

    // 申请内容
    val greetingMessage: String,        // 打招呼内容
    val remark: String?,                // 备注
    val tags: String?,                  // 标签
    val note: String?,                  // 备忘

    // 状态
    val status: RequestStatus,          // 状态
    val direction: RequestDirection,    // 方向（发出/收到）

    // 时间
    val createAt: Long,                // 创建时间
    val updatedAt: Long                 // 更新时间
) {
    fun toDomain(): FriendRequest {
        return FriendRequest(
            requestId = requestId,
            fromUserId = fromUserId,
            fromNickname = fromNickname,
            fromAvatarPath = fromAvatarPath,
            greetingMessage = greetingMessage,
            remark = remark,
            status = status,
            timestamp = createAt
        )
    }
}

enum class RequestStatus {
    PENDING,    // 待处理
    ACCEPTED,   // 已接受
    REJECTED,   // 已拒绝
    EXPIRED     // 已过期
}

enum class RequestDirection {
    OUTGOING,   // 发出的
    INCOMING    // 收到的
}