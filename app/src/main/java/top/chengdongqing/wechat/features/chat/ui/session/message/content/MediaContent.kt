package top.chengdongqing.wechat.features.chat.ui.session.message.content

import android.util.Size
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import top.chengdongqing.wechat.core.designsystem.components.progress.WeCircleProgress
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.rememberScreenFractionWidth
import top.chengdongqing.wechat.core.util.format
import top.chengdongqing.wechat.core.util.loadMediaThumbnail
import top.chengdongqing.wechat.core.util.toPercent
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.model.MessageSendStatus
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatSessionContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MediaContent(message: ChatMessage) {
    val targetWidth = rememberScreenFractionWidth()
    val content = message.content as MessageContent.Media
    val chatContext = LocalChatSessionContext.current

    Box(
        modifier = Modifier
            .heightIn(max = targetWidth)
            .widthIn(max = targetWidth)
            .then(if (content.ratio > 0) Modifier.aspectRatio(content.ratio) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // 缩略图
        ThumbnailImage(content.localPath, content is MessageContent.Video)

        when (content) {
            is MessageContent.Image -> {
                // 发送的进度
                if (message.isSending) {
                    SendingOverlay(message.sendProgress)
                }
            }

            is MessageContent.Video -> {
                VideoOverlay(
                    message = message,
                    durationText = content.duration.milliseconds.format(),
                    onActionClick = {
                        chatContext?.onStopTransfer(message.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThumbnailImage(localPath: String, isVideo: Boolean) {
    val context = LocalContext.current
    val thumbnail by produceState<Any?>(initialValue = null, localPath) {
        value = context.loadMediaThumbnail(File(localPath).toUri(), isVideo, Size(1200, 1200))
    }

    AsyncImage(
        model = thumbnail,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = if (thumbnail == null) 0f else 1f
    )
}

@Composable
private fun SendingOverlay(progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WeLoading(size = 42.dp, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        Text(progress.toPercent(), color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun BoxScope.VideoOverlay(
    message: ChatMessage,
    durationText: String,
    onActionClick: () -> Unit
) {
    // 进度显示层
    if (message.isSending) {
        Box(
            modifier = Modifier
                .size(39.dp)
                .clip(CircleShape)
                .clickable { onActionClick() },
            contentAlignment = Alignment.Center
        ) {
            WeCircleProgress(
                percent = message.sendProgress * 100,
                size = 36.dp,
                strokeWidth = 3.dp,
                trackColor = Color.LightGray.copy(alpha = 0.8f),
                indicatorColor = White,
                formatter = null
            )
            val icon = if (message.sendStatus is MessageSendStatus.Paused)
                R.drawable.ic_play_filled else R.drawable.ic_pause_filled
            Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_play_filled),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .border(1.dp, Color.White, CircleShape)
                .padding(4.dp)
        )
    }

    // 时长层
    Text(
        text = durationText,
        color = Color.White,
        fontSize = 10.sp,
        modifier = Modifier
            .padding(8.dp)
            .align(Alignment.BottomEnd)
    )
}