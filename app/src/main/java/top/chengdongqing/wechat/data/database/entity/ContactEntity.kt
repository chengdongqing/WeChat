package top.chengdongqing.wechat.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import top.chengdongqing.wechat.features.me.domain.model.Gender

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val userId: String,

    val nickname: String,
    val avatarPath: String?,
    val signature: String? = null,
    val gender: Gender? = null,

    // 备注信息
    val remarkName: String? = null,
    val tags: String? = null,
    val note: String? = null,

    // 是否拉黑
    val isBlocked: Boolean = false,

    // 添加方式
    val source: AddSource? = null,
    val isFromMe: Boolean = true,

    // 时间
    val addedAt: Long,
    val updatedAt: Long
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