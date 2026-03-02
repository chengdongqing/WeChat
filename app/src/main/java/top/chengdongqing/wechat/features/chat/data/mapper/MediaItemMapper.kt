package top.chengdongqing.wechat.features.chat.data.mapper

import androidx.core.net.toUri
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import java.io.File

fun MessageContent.Media.toMediaItem() = MediaItem(
    uri = File(localPath).toUri(),
    filename = filename,
    mediaType = if (this is MessageContent.Video) MediaType.Video else MediaType.Image,
    mimeType = mimeType,
    width = width,
    height = height,
    duration = if (this is MessageContent.Video) duration else 0
)