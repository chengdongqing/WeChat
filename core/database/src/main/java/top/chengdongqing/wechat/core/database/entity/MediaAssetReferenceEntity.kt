package top.chengdongqing.wechat.core.database.entity

import androidx.room3.Entity
import androidx.room3.Index

@Entity(
    tableName = "media_asset_references",
    primaryKeys = ["assetPath", "ownerType", "ownerId"],
    indices = [
        Index(value = ["ownerType", "ownerId"]),
        Index(value = ["assetPath"])
    ]
)
data class MediaAssetReferenceEntity(
    val assetPath: String,
    val ownerType: String,
    val ownerId: String,
    val createdAt: Long = System.currentTimeMillis()
)
