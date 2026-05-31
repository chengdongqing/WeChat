package top.chengdongqing.wechat.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "media_files")
data class MediaFileEntity(
    @PrimaryKey
    val localPath: String,           // 文件路径
    val refCount: Int = 1,           // 引用计数
    val checksum: String,            // 文件哈希值
    val createdAt: Long = System.currentTimeMillis()
)