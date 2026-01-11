package top.chengdongqing.wechat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import top.chengdongqing.wechat.core.util.AppJson
import top.chengdongqing.wechat.data.model.ChatPayload

@Database(entities = [MessageEntity::class], version = 2, exportSchema = true)
@TypeConverters(ChatPayloadConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}

object DatabaseModule {
    private var db: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "wechat_db"
            )
                .fallbackToDestructiveMigration(false)
                .build()
            db = instance
            instance
        }
    }
}

class ChatPayloadConverter {
    @TypeConverter
    fun fromPayload(payload: ChatPayload): String {
        return AppJson.instance.encodeToString(payload)
    }

    // 将数据库里的字符串转回对象
    @TypeConverter
    fun toPayload(json: String): ChatPayload {
        return AppJson.instance.decodeFromString(json)
    }
}

/**
 * exportSchema = true 在你的项目目录下生成一个 .json 文件，详细记录数据库的所有表结构、列名、类型以及索引。
 *
 * 为什么需要它：
 * 1. 版本迁移 (Migration)： 当你以后想给 messages 表增加一个字段时，Room 需要对比新旧 JSON 文件来确保迁移逻辑正确。如果没有这个文件，自动迁移可能会失效。
 * 2. 代码审查： 你可以直接在文件夹里看到数据库结构的变更历史。
 */