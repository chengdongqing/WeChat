package top.chengdongqing.wechat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import top.chengdongqing.wechat.core.util.AppJson
import top.chengdongqing.wechat.core.util.ImageUtils.decodeBase64ToBitmap
import top.chengdongqing.wechat.core.util.formatTime
import top.chengdongqing.wechat.data.local.MessageEntity
import top.chengdongqing.wechat.data.model.ChatPayload

@Composable
fun MessageBubble(message: MessageEntity) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val color =
        if (message.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor =
        if (message.isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    // 提前解析 Payload
    val payload = remember(message.payloadJson) {
        runCatching { AppJson.instance.decodeFromString<ChatPayload>(message.payloadJson) }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(if (payload is ChatPayload.Media) 4.dp else 12.dp)) {
                when (payload) {
                    is ChatPayload.Text -> {
                        Text(text = payload.content, color = textColor)
                    }

                    is ChatPayload.Media -> {
                        // 渲染媒体组件（图片/缩略图）
                        MediaContent(payload, message)
                    }

                    is ChatPayload.Location -> {
                        Text(text = "📍 ${payload.address}", color = textColor)
                    }

                    else -> {
                        // 解析失败或未知类型的保底显示
                        Text(text = message.payloadJson, color = textColor)
                    }
                }

                // 只有文本消息才在下方显示“发送中”文字，图片消息的进度在图上显示
                if (message.isFromMe && payload is ChatPayload.Text) {
                    StatusText(message.status, textColor)
                }
            }
        }
        Text(
            text = formatTime(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun MediaContent(payload: ChatPayload.Media, message: MessageEntity) {
    Box(contentAlignment = Alignment.Center) {
        // 1. 图片展示层
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(payload.localPath ?: decodeBase64ToBitmap(payload.thumbBase64)) // 优先本地图，其次缩略图
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            // 正在发送/接收时，让图片变暗
            alpha = if (message.status == 0) 0.6f else 1f
        )

        // 2. 进度/状态层
        if (message.status == 0) { // 发送中或下载中
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 如果 payload 里有具体的 progress 数值
                CircularProgressIndicator(
                    progress = { message.progress }, // 需要你在 Media 类里增加 progress 字段
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (message.status == 2) { // 失败
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "失败",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

@Composable
private fun StatusText(status: Int, textColor: Color) {
    val statusInfo = when (status) {
        0 -> "发送中..."
        2 -> "失败 ⚠️"
        else -> ""
    }

    if (statusInfo.isNotEmpty()) {
        Text(
            text = statusInfo,
            style = MaterialTheme.typography.labelSmall,
            color = if (status == 2) MaterialTheme.colorScheme.error else textColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}