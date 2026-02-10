package top.chengdongqing.wechat.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import top.chengdongqing.wechat.data.model.Gender

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

    // 时间
    val addedAt: Long,
    val updatedAt: Long
)