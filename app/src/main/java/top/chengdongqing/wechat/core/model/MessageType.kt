package top.chengdongqing.wechat.core.model

import android.content.Context
import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.R

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
    Favorite,       // 收藏
    Music,          // 音乐
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

/**
 * 获取消息预览文本
 */
fun MessageType.toPreviewText(context: Context, content: String): String = when (this) {
    MessageType.Text -> content
    MessageType.Image -> context.getString(R.string.message_preview_image)
    MessageType.Voice -> context.getString(R.string.message_preview_voice)
    MessageType.Video -> context.getString(R.string.message_preview_video)
    MessageType.File -> context.getString(R.string.message_preview_file)
    MessageType.Location -> context.getString(R.string.message_preview_location)
    MessageType.Favorite -> context.getString(R.string.message_preview_favorite)
    MessageType.ContactCard -> context.getString(R.string.message_preview_contact_card)
    MessageType.Sticker -> context.getString(R.string.message_preview_sticker)
    MessageType.Music -> context.getString(R.string.message_preview_music)
    MessageType.VoiceCall -> context.getString(R.string.message_preview_voice_call)
    MessageType.VideoCall -> context.getString(R.string.message_preview_video_call)
}