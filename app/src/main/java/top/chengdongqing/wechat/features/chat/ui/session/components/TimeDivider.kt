package top.chengdongqing.wechat.features.chat.ui.session.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.util.toChatDisplayTime
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage

@Composable
fun TimeDivider(
    messages: List<ChatMessage>,
    index: Int
) {
    // 是否显示时间
    val shouldShow by remember(index, messages.size) {
        derivedStateOf {
            val message = messages[index]
            // 在 reverseLayout 中，index 最大的那条是时间轴上的第一条（最旧的消息）
            if (index == messages.lastIndex) {
                true
            } else {
                // index + 1 是逻辑上的上一条消息（更旧的那条）
                val prevMessage = messages[index + 1]
                message.timestamp - prevMessage.timestamp > 5 * 60 * 1000
            }
        }
    }

    if (shouldShow) {
        val message = messages[index]
        // 时间格式化
        val time = remember(message.timestamp) {
            message.timestamp.toChatDisplayTime()
        }

        TimeText(time)
    }
}

@Composable
private fun TimeText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                color = Color.Gray
            )
        )
    }
}