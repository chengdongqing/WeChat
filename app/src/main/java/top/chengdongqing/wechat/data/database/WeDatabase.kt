package top.chengdongqing.wechat.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.database.entity.MessageEntity

@Database(
    entities = [
        FriendRequestEntity::class,
        ContactEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        ConnectionInfoEntity::class
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
}