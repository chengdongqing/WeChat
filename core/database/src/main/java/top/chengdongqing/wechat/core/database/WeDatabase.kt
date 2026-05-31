package top.chengdongqing.wechat.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.dao.ContactDao
import top.chengdongqing.wechat.core.database.dao.FriendRequestDao
import top.chengdongqing.wechat.core.database.dao.MediaFileDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import top.chengdongqing.wechat.core.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.core.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.core.database.entity.MediaFileEntity
import top.chengdongqing.wechat.core.database.entity.MessageEntity

@Database(
    entities = [
        FriendRequestEntity::class,
        ContactEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        ConnectionInfoEntity::class,
        MediaFileEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class WeDatabase : RoomDatabase() {
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun contactDao(): ContactDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun connectionInfoDao(): ConnectionInfoDao
    abstract fun mediaFileDao(): MediaFileDao
}