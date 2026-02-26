package top.chengdongqing.wechat.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
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

    val source: AddSource? = null,        // 添加方式
    val isFromMe: Boolean = true,         // 是否我主动添加

    @Embedded
    val audit: EntityAudit = EntityAudit()
) {
    val displayName: String
        get() = remarkName ?: nickname
}

enum class AddSource(val label: String) {
    Search("搜索账号"),
    QRCode("扫一扫"),
    Radar("雷达扫描"),
    Group("群聊"),
    Card("名片分享");

    fun getDescription(isFromMe: Boolean): String = when (isFromMe) {
        true -> "通过${label}添加"
        false -> "对方通过${label}添加"
    }
}