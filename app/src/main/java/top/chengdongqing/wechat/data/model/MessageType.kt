package top.chengdongqing.wechat.data.model

import kotlinx.serialization.Serializable

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
    Favorite,       // 收藏
    Music,          // 音乐
    VoiceCall,      // 语音通话记录
    VideoCall;      // 视频通话记录

    /**
     * 是否为通话消息
     */
    val isCallMessage: Boolean
        get() = this == VideoCall || this == VoiceCall

    /**
     * 是否允许转发
     */
    val isForwardable: Boolean
        get() = when (this) {
            VoiceCall, VideoCall -> false
            else -> true
        }

    /**
     * 是否需要解析json来获取文件名
     */
    val isFileNameInJson: Boolean
        get() = when (this) {
            Image, Video, File -> true
            else -> false
        }
}

fun MessageType.toPreviewText(content: String): String = when (this) {
    MessageType.Text -> content
    MessageType.Image -> "[图片]"
    MessageType.Voice -> "[语音]"
    MessageType.Video -> "[视频]"
    MessageType.File -> "[文件]"
    MessageType.Location -> "[位置]"
    MessageType.Favorite -> "[收藏]"
    MessageType.ContactCard -> "[名片]"
    MessageType.Sticker -> "[表情]"
    MessageType.Music -> "[音乐]"
    MessageType.VoiceCall -> "[语音通话]"
    MessageType.VideoCall -> "[视频通话]"
}