package top.chengdongqing.wechat.data.database

import androidx.room.TypeConverter
import top.chengdongqing.wechat.data.model.ContactAddSource
import top.chengdongqing.wechat.data.model.FriendRequestStatus
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.features.me.domain.model.Gender

class DatabaseConverters {

    @TypeConverter
    fun fromRequestStatus(value: FriendRequestStatus): String = value.name

    @TypeConverter
    fun toRequestStatus(value: String): FriendRequestStatus = FriendRequestStatus.valueOf(value)

    @TypeConverter
    fun fromGender(value: Gender): String = value.name

    @TypeConverter
    fun toGender(value: String): Gender = Gender.valueOf(value)

    @TypeConverter
    fun fromAddSource(value: ContactAddSource) = value.name

    @TypeConverter
    fun toAddSource(value: String): ContactAddSource = ContactAddSource.valueOf(value)

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