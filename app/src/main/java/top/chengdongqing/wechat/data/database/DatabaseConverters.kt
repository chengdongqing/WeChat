package top.chengdongqing.wechat.data.database

import androidx.room.TypeConverter
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.data.model.Gender
import top.chengdongqing.wechat.data.model.Gender.Companion.getIndex

class DatabaseConverters {

    @TypeConverter
    fun fromRequestStatus(value: RequestStatus): String {
        return value.name
    }

    @TypeConverter
    fun toRequestStatus(value: String): RequestStatus {
        return RequestStatus.valueOf(value)
    }

    @TypeConverter
    fun fromRequestDirection(value: RequestDirection): String {
        return value.name
    }

    @TypeConverter
    fun toRequestDirection(value: String): RequestDirection {
        return RequestDirection.valueOf(value)
    }

    @TypeConverter
    fun fromGender(value: Gender): Int {
        return value.getIndex()
    }

    // 将数据库里的字符串转回对象
    @TypeConverter
    fun toGender(value: Int): Gender? {
        return Gender.fromIndex(value)
    }
}