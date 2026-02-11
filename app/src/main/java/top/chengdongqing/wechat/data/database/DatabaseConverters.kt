package top.chengdongqing.wechat.data.database

import androidx.room.TypeConverter
import top.chengdongqing.wechat.data.database.entity.AddSource
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.data.model.Gender
import top.chengdongqing.wechat.data.model.Gender.Companion.getIndex

class DatabaseConverters {

    @TypeConverter
    fun fromRequestStatus(value: RequestStatus): String = value.name

    @TypeConverter
    fun toRequestStatus(value: String): RequestStatus = RequestStatus.valueOf(value)

    @TypeConverter
    fun fromRequestDirection(value: RequestDirection): String = value.name

    @TypeConverter
    fun toRequestDirection(value: String): RequestDirection = RequestDirection.valueOf(value)

    @TypeConverter
    fun fromGender(value: Gender): Int = value.getIndex()

    @TypeConverter
    fun toGender(value: Int): Gender? = Gender.fromIndex(value)

    @TypeConverter
    fun fromAddSource(value: AddSource) = value.name

    @TypeConverter
    fun toAddSource(value: String): AddSource = AddSource.valueOf(value)
}