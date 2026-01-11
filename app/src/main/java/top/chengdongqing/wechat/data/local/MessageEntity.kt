package top.chengdongqing.wechat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import top.chengdongqing.wechat.data.model.ChatPayload

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,            // 消息唯一ID (UUID)
    val chatId: String,        // 存对方ID
    val senderId: String,      // 发送者标识
    val senderName: String,    // 发送者昵称
    val payload: ChatPayload,   // 消息详细内容
    val msgType: String,       // 冗余字段：TEXT, IMAGE, VOICE 等，方便快速筛选
    val isFromMe: Boolean,     // 是否是我发送的
    val status: Int = 0,        // 0: 发送中, 1: 成功, 2: 失败, 3: 已撤回
    val progress: Float = 1f,  // 新增：0.0 ~ 1.0
    val timestamp: Long = System.currentTimeMillis(),       // 发送时间
)