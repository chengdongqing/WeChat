package top.chengdongqing.wechat.core.model

import kotlinx.serialization.Serializable

/**
 * 消息类型
 */
@Serializable
enum class MessageType {
    Text,           // 文本
    Voice,          // 语音
    Image,          // 图片
    Video,          // 视频
    File,           // 文件
    Sticker,        // 表情
    Location,       // 位置
    ContactCard,    // 名片
    Music,          // 音乐
    Live,           // 群直播
    VoiceCall,      // 语音通话记录
    VideoCall;      // 视频通话记录

    /**
     * 是否为通话消息
     */
    val isCallMessage: Boolean
        get() = this in setOf(VideoCall, VoiceCall)

    /**
     * 是否允许转发
     */
    val isForwardable: Boolean
        get() = !isCallMessage
}
