package top.chengdongqing.wechat.core.database.di

import android.content.Context
import androidx.room3.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.WeDatabaseMigrations
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.dao.ContactDao
import top.chengdongqing.wechat.core.database.dao.ContactTagDao
import top.chengdongqing.wechat.core.database.dao.FriendRequestDao
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.database.dao.MediaAssetReferenceDao
import top.chengdongqing.wechat.core.database.dao.MediaFileDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import javax.inject.Singleton

/** Database wiring belongs to the module that owns the schema and migrations. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWeDatabase(@ApplicationContext context: Context): WeDatabase =
        Room.databaseBuilder(context, WeDatabase::class.java, DATABASE_NAME)
            .addMigrations(*WeDatabaseMigrations.all)
            .build()

    @Provides
    fun provideFriendRequestDao(database: WeDatabase): FriendRequestDao = database.friendRequestDao()

    @Provides
    fun provideContactDao(database: WeDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideChatSessionDao(database: WeDatabase): ChatSessionDao = database.chatSessionDao()

    @Provides
    fun provideMessageDao(database: WeDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideConnectionInfoDao(database: WeDatabase): ConnectionInfoDao = database.connectionInfoDao()

    @Provides
    fun provideMediaFileDao(database: WeDatabase): MediaFileDao = database.mediaFileDao()

    @Provides
    fun provideMediaAssetReferenceDao(database: WeDatabase): MediaAssetReferenceDao =
        database.mediaAssetReferenceDao()

    @Provides
    fun provideGroupDao(database: WeDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideContactTagDao(database: WeDatabase): ContactTagDao = database.contactTagDao()

    private const val DATABASE_NAME = "wechat"
}
