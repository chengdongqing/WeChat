package top.chengdongqing.wechat.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
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
            "wechat_database"
        )
            .fallbackToDestructiveMigration(false)  // 开发阶段可以用，生产环境需要配置 Migration
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
}