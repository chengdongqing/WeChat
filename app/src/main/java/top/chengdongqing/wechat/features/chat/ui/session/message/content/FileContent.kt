package top.chengdongqing.wechat.features.chat.ui.session.message.content

import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.progress.WeCircleProgress
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.model.MessageSendStatus
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatSessionContext

@Composable
fun FileContent(message: ChatMessage) {
    val context = LocalContext.current
    val content = message.content as MessageContent.File

    val icon = if (message.isProgressing) {
        R.drawable.ic_file_placeholder_filled
    } else {
        R.drawable.ic_file_filled
    }

    Row(
        modifier = Modifier.padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = content.filename,
                fontSize = 16.sp,
                color = WeTheme.colorScheme.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFileSize(context, content.size),
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )

            if (message.isProgressing) {
                ControlWithProgress(message)
            }
        }
    }
}

@Composable
fun ControlWithProgress(message: ChatMessage) {
    val chatContext = LocalChatSessionContext.current
    val isPaused = message.sendStatus is MessageSendStatus.Paused

    val icon = if (isPaused) R.drawable.ic_play_filled else R.drawable.ic_pause_filled

    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable {
                if (isPaused) {
                    chatContext?.onResumeTransfer(message.id)
                } else {
                    chatContext?.onPauseTransfer(message.id)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        WeCircleProgress(
            percent = message.sendProgress * 100,
            size = 24.dp,
            strokeWidth = 2.dp,
            trackColor = Color.LightGray.copy(alpha = 0.8f),
            indicatorColor = Color.Gray
        )

        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(14.dp)
        )
    }
}