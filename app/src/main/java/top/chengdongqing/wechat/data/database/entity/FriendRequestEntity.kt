package top.chengdongqing.wechat.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import top.chengdongqing.wechat.data.model.FriendRequestStatus

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey
    val id: String,                     // 请求ID

    val userId: String,                 // 对方的ID
    val nickname: String,               // 对方的昵称
    val avatarPath: String?,            // 对方的头像路径
    val publicKey: String?,             // 对方的公钥

    val greeting: String,               // 打招呼内容
    val remark: String? = null,         // 备注
    val note: String? = null,           // 备忘

    val status: FriendRequestStatus,    // 状态
    val isFromMe: Boolean = false,      // 是否我发出的
    val isRead: Boolean = false,        // 是否已读

    @Embedded
    val audit: EntityAudit = EntityAudit()
)