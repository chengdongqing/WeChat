package top.chengdongqing.wechat.core.designsystem.ui

import android.content.Context
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.model.MessageType

fun MessageType.toPreviewText(context: Context, content: String): String = when (this) {
    MessageType.Text -> content
    MessageType.Image -> context.getString(R.string.message_preview_image)
    MessageType.Voice -> context.getString(R.string.message_preview_voice)
    MessageType.Video -> context.getString(R.string.message_preview_video)
    MessageType.File -> context.getString(R.string.message_preview_file)
    MessageType.Location -> context.getString(R.string.message_preview_location)
    MessageType.LiveLocation -> context.getString(R.string.message_preview_live_location)
    MessageType.ContactCard -> context.getString(R.string.message_preview_contact_card)
    MessageType.Sticker -> context.getString(R.string.message_preview_sticker)
    MessageType.Music -> context.getString(R.string.message_preview_music)
    MessageType.Live -> context.getString(R.string.message_preview_live)
    MessageType.ChatHistory -> context.getString(R.string.message_preview_chat_history)
    MessageType.VoiceCall -> context.getString(R.string.message_preview_voice_call)
    MessageType.VideoCall -> context.getString(R.string.message_preview_video_call)
}
