package top.chengdongqing.wechat.core.di

import android.content.Context
import androidx.room3.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.dao.ContactDao
import top.chengdongqing.wechat.core.database.dao.FriendRequestDao
import top.chengdongqing.wechat.core.database.dao.MediaFileDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWeDatabase(
        @ApplicationContext context: Context
    ): WeDatabase {
        return Room.databaseBuilder(
            context,
            WeDatabase::class.java,
            "wechat"
        )
            .fallbackToDestructiveMigration(true)  // 生产环境需要配置 Migration
            .build()
    }

    @Provides
    @Singleton
    fun provideFriendRequestDao(database: WeDatabase): FriendRequestDao {
        return database.friendRequestDao()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: WeDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    @Singleton
    fun provideChatSessionDao(database: WeDatabase): ChatSessionDao {
        return database.chatSessionDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: WeDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideConnectionInfoDao(database: WeDatabase): ConnectionInfoDao {
        return database.connectionInfoDao()
    }

    @Provides
    @Singleton
    fun provideMediaFileDao(database: WeDatabase): MediaFileDao {
        return database.mediaFileDao()
    }
}