package top.chengdongqing.wechat.core.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.dao.ContactDao
import top.chengdongqing.wechat.core.database.dao.ContactTagDao
import top.chengdongqing.wechat.core.database.dao.FriendRequestDao
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.database.dao.MediaAssetReferenceDao
import top.chengdongqing.wechat.core.database.dao.MediaFileDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import top.chengdongqing.wechat.core.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.core.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.database.entity.ContactTagEntity
import top.chengdongqing.wechat.core.database.entity.ContactTagMemberEntity
import top.chengdongqing.wechat.core.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.core.database.entity.GroupEntity
import top.chengdongqing.wechat.core.database.entity.GroupMemberEntity
import top.chengdongqing.wechat.core.database.entity.MediaAssetReferenceEntity
import top.chengdongqing.wechat.core.database.entity.MediaFileEntity
import top.chengdongqing.wechat.core.database.entity.MessageEntity

@Database(
    entities = [
        FriendRequestEntity::class,
        ContactEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        ConnectionInfoEntity::class,
        MediaFileEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        ContactTagEntity::class,
        ContactTagMemberEntity::class,
        MediaAssetReferenceEntity::class
    ],
    version = 6,
    exportSchema = true
)
@ColumnTypeConverters(DatabaseConverters::class)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class WeDatabase : RoomDatabase() {
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun contactDao(): ContactDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun connectionInfoDao(): ConnectionInfoDao
    abstract fun mediaFileDao(): MediaFileDao
    abstract fun mediaAssetReferenceDao(): MediaAssetReferenceDao
    abstract fun groupDao(): GroupDao
    abstract fun contactTagDao(): ContactTagDao
}
