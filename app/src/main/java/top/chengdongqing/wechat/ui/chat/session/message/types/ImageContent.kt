package top.chengdongqing.wechat.ui.chat.session.message.types

import android.util.Size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.utils.loadMediaThumbnail
import top.chengdongqing.wechat.data.model.MediaItem
import top.chengdongqing.wechat.data.model.MediaType
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.components.media.preview.previewMedias
import top.chengdongqing.wechat.ui.utils.rememberWindowFractionWidth

@Composable
fun ImageContent(content: MessageContent.Image) {
    val context = LocalContext.current
    val targetWidth = rememberWindowFractionWidth()

    val media = remember {
        MediaItem(
            uri = content.url.toUri(),
            filename = content.filename,
            mediaType = MediaType.IMAGE,
            mimeType = content.mimeType,
            width = content.width,
            height = content.height
        )
    }

    val thumbnail by produceState<Any?>(initialValue = null) {
        value = context.loadMediaThumbnail(uri = media.uri, size = Size(1200, 1200))
    }

    AsyncImage(
        model = thumbnail,
        contentDescription = null,
        modifier = Modifier
            .heightIn(max = targetWidth)
            .widthIn(max = targetWidth)
            .aspectRatio(content.ratio)
            .clickable {
                context.previewMedias(listOf(media), 0)
            },
        contentScale = ContentScale.Crop
    )
}