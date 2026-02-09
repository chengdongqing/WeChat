package top.chengdongqing.wechat.data.database

import androidx.room.TypeConverter
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus

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
}