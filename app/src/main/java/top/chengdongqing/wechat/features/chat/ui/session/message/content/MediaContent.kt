package top.chengdongqing.wechat.features.chat.ui.session.message.content

import android.util.Size
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaType
import top.chengdongqing.wechat.core.designsystem.components.media.preview.previewMedias
import top.chengdongqing.wechat.core.designsystem.components.progress.WeCircleProgress
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.rememberScreenFractionWidth
import top.chengdongqing.wechat.core.util.format
import top.chengdongqing.wechat.core.util.loadMediaThumbnail
import top.chengdongqing.wechat.core.util.toPercent
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.model.MessageSendStatus
import top.chengdongqing.wechat.features.chat.ui.session.LocalMediaContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MediaContent(message: ChatMessage) {
    val context = LocalContext.current
    val targetWidth = rememberScreenFractionWidth()
    val content = message.content as MessageContent.Media
    val isVideo = content is MessageContent.Video

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

        when (content) {
            is MessageContent.Image -> {
                // 发送的进度
                if (message.isSending) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        WeLoading(size = 42.dp, color = White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            message.sendProgress.toPercent(),
                            color = White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            is MessageContent.Video -> {
                if (message.isSending) {
                    Box(
                        modifier = Modifier
                            .size(39.dp)
                            .clip(CircleShape)
                            .clickable {},
                        contentAlignment = Alignment.Center
                    ) {
                        WeCircleProgress(
                            message.sendProgress * 100,
                            size = 36.dp,
                            strokeWidth = 3.dp,
                            trackColor = Color.LightGray.copy(alpha = 0.8f),
                            indicatorColor = White,
                            formatter = null
                        )

                        when (message.sendStatus) {
                            is MessageSendStatus.Sending -> {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pause_filled),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            is MessageSendStatus.Paused -> {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_filled),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            else -> {}
                        }
                    }
                } else {
                    // 播放图标
                    Icon(
                        painter = painterResource(R.drawable.ic_play_filled),
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                            .padding(4.dp)
                    )
                }

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
}

private fun MessageContent.Media.toMediaItem() = MediaItem(
    uri = localPath.toUri(),
    filename = filename,
    mediaType = if (this is MessageContent.Video) MediaType.Video else MediaType.Image,
    mimeType = mimeType,
    width = width,
    height = height,
    duration = if (this is MessageContent.Video) duration else 0
)