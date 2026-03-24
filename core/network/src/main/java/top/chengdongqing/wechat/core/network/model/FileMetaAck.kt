package top.chengdongqing.wechat.core.network.model

import kotlinx.serialization.Serializable

/**
 * 文件元数据应答状态
 */
enum class FileAckStatus {
    /** 准备接收，从头传输 */
    ReadyToReceive,

    /** 断点续传，从 receivedBytes 处继续 */
    ResumeFrom,

    /** 文件已存在（checksum 匹配），无需传输 */
    AlreadyExists
}

/**
 * 文件元数据应答包
 */
@Serializable
data class FileMetaAck(
    /** 对应的消息 ID */
    val messageId: String,

    /** 发送此 ACK 的用户 ID（即文件接收方） */
    val senderId: String,

    /** ACK 的接收方（即文件发送方） */
    val receiverId: String,

    /** 应答状态 */
    val status: FileAckStatus,

    /** 已接收字节数（仅 ResumeFrom 时有效） */
    val receivedBytes: Long = 0,

    /** 时间戳 */
    val timestamp: Long = System.currentTimeMillis()
)