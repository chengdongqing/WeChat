package top.chengdongqing.wechat.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val userId: String,

    val nickname: String,
    val avatarPath: String?,
    val signature: String?,
    val gender: Int,

    // 备注信息
    val remarkName: String?,
    val tags: String?,
    val note: String?,

    // 时间
    val addedAt: Long,
    val updatedAt: Long
)