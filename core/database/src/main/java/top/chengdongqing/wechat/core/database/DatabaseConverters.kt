package top.chengdongqing.wechat.core.database

import androidx.room3.ColumnTypeConverter
import top.chengdongqing.wechat.core.model.ContactAddSource
import top.chengdongqing.wechat.core.model.FriendRequestStatus
import top.chengdongqing.wechat.core.model.Gender
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.model.SendStatus

class DatabaseConverters {

    @ColumnTypeConverter
    fun fromRequestStatus(value: FriendRequestStatus): String = value.name

    @ColumnTypeConverter
    fun toRequestStatus(value: String): FriendRequestStatus = FriendRequestStatus.valueOf(value)

    @ColumnTypeConverter
    fun fromGender(value: Gender): String = value.name

    @ColumnTypeConverter
    fun toGender(value: String): Gender = Gender.valueOf(value)

    @ColumnTypeConverter
    fun fromAddSource(value: ContactAddSource) = value.name

    @ColumnTypeConverter
    fun toAddSource(value: String): ContactAddSource = ContactAddSource.valueOf(value)

    @ColumnTypeConverter
    fun fromMessageType(value: MessageType) = value.name

    @ColumnTypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)

    @ColumnTypeConverter
    fun fromSendStatus(value: SendStatus) = value.name

    @ColumnTypeConverter
    fun toSendStatus(value: String): SendStatus = SendStatus.valueOf(value)

    @ColumnTypeConverter
    fun fromSendError(value: SendError) = value.name

    @ColumnTypeConverter
    fun toSendError(value: String): SendError = SendError.valueOf(value)
}