package top.chengdongqing.wechat.ui.chat.session.message.content

import android.util.Size
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.format
import top.chengdongqing.wechat.core.util.loadMediaThumbnail
import top.chengdongqing.wechat.data.model.MediaItem
import top.chengdongqing.wechat.data.model.MediaType
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.LocalMediaContext
import top.chengdongqing.wechat.ui.components.media.preview.previewMedias
import top.chengdongqing.wechat.ui.util.rememberScreenFractionWidth
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MediaContent(content: MessageContent.Media) {
    val context = LocalContext.current
    val isVideo = content is MessageContent.Video
    val targetWidth = rememberScreenFractionWidth()

    // 获取更多媒体数据，方便预览时切换
    val mediaContext = LocalMediaContext.current
    val (mediaItems, currentIndex) = remember(content, mediaContext) {
        val items = mediaContext?.allMedia?.map { it.toMediaItem() }
            ?: listOf(content.toMediaItem())
        val index = mediaContext?.getIndexOf(content) ?: 0
        items to index
    }
    val media = mediaItems[currentIndex]

    // 异步加载缩略图
    val thumbnail by produceState<Any?>(initialValue = null, content) {
        value = context.loadMediaThumbnail(
            uri = media.uri,
            isVideo = isVideo,
            size = Size(1200, 1200)
        )
    }

    Box(
        modifier = Modifier
            .heightIn(max = targetWidth)
            .widthIn(max = targetWidth)
            .then(if (content.ratio > 0) Modifier.aspectRatio(content.ratio) else Modifier)
            .clickable {
                context.previewMedias(mediaItems, currentIndex)
            },
        contentAlignment = Alignment.Center
    ) {
        // 缩略图
        AsyncImage(
            model = thumbnail,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 视频专属UI
        if (content is MessageContent.Video) {
            // 播放图标
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow_filled),
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .padding(4.dp)
            )

            // 视频时长
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = content.duration.milliseconds.format(),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun MessageContent.Media.toMediaItem() = MediaItem(
    uri = uri,
    filename = filename,
    mediaType = if (this is MessageContent.Video) MediaType.Video else MediaType.Image,
    mimeType = mimeType,
    width = width,
    height = height,
    duration = if (this is MessageContent.Video) duration else 0
)