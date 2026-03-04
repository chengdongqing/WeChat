package top.chengdongqing.wechat.data.database

import androidx.room.TypeConverter
import top.chengdongqing.wechat.data.database.entity.ConnectionType
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.data.model.ContactAddSource
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.features.me.domain.model.Gender
import top.chengdongqing.wechat.features.me.domain.model.Gender.Companion.getIndex

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
    fun fromAddSource(value: ContactAddSource) = value.name

    @TypeConverter
    fun toAddSource(value: String): ContactAddSource = ContactAddSource.valueOf(value)

    @TypeConverter
    fun fromConnectionType(value: ConnectionType) = value.name

    @TypeConverter
    fun toConnectionType(value: String): ConnectionType = ConnectionType.valueOf(value)

    @TypeConverter
    fun fromMessageType(value: MessageType) = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)

    @TypeConverter
    fun fromSendStatus(value: SendStatus) = value.name

    @TypeConverter
    fun toSendStatus(value: String): SendStatus = SendStatus.valueOf(value)

    @TypeConverter
    fun fromSendError(value: SendError) = value.name

    @TypeConverter
    fun toSendError(value: String): SendError = SendError.valueOf(value)
}