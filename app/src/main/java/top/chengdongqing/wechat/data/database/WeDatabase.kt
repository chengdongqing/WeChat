package top.chengdongqing.wechat.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity

@Database(
    entities = [
        FriendRequestEntity::class,
        ContactEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class WeDatabase : RoomDatabase() {
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun contactDao(): ContactDao
}