package top.chengdongqing.wechat.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import top.chengdongqing.wechat.data.model.ContactAddSource
import top.chengdongqing.wechat.features.me.domain.model.Gender

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,                       // 联系人id

    val nickname: String,                 // 昵称
    val avatarPath: String?,              // 头像
    val signature: String? = null,        // 签名
    val gender: Gender? = null,           // 性别

    val remarkName: String? = null,       // 备注名
    val note: String? = null,             // 备忘

    val isBlocked: Boolean = false,       // 是否拉黑

    val source: ContactAddSource? = null, // 添加方式
    val isFromMe: Boolean = true,         // 是否我主动添加

    val publicKey: String? = null,        // 证明身份的公钥
    val version: Long,                    // 版本号，用于比对资料是否需要更新

    @Embedded
    val audit: EntityAudit = EntityAudit()
) {
    val displayName: String
        get() = remarkName ?: nickname
}