package top.chengdongqing.wechat.core.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["createdAt"]), Index(value = ["type"])]
)
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val content: String,
    val mediaPaths: String = "",
    val sourceMessageIds: String = "",
    val sourceName: String = "",
    val createdAt: Long,
    val updatedAt: Long
)
