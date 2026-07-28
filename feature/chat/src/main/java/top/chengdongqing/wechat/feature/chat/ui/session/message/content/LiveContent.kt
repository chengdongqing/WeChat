package top.chengdongqing.wechat.feature.chat.ui.session.message.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext

@Composable
fun LiveContent(message: ChatMessage) {
    val content = message.content as MessageContent.Live
    val context = LocalChatSessionContext.current
    if (content.status != "live" && content.status != "ended") {
        val label = when (content.status) {
            "joined" -> "${content.hostName}进入了直播间"
            "left" -> "${content.hostName}离开了直播间"
            else -> "直播状态已更新"
        }
        Text(
            label,
            color = Color.White.copy(alpha = .7f),
            fontSize = 13.sp,
            modifier = Modifier.background(Color(0x55000000), RoundedCornerShape(5.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
        return
    }
    Column(
        Modifier.width(235.dp)
            .background(Color(0xFF242424), RoundedCornerShape(8.dp))
            .clickable(enabled = content.status == "live") {
                context?.onNavigateToLive(
                    content.liveId,
                    message.isFromMe,
                    content.actorId ?: message.senderId
                )
            }
    ) {
        Box(
            Modifier.fillMaxWidth().height(112.dp).background(Color(0xFF343434)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_video_filled),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
            Text(
                if (content.status == "live") "直播中" else "直播已结束",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    .background(Color(0xFFE94343), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Column(Modifier.padding(12.dp)) {
            Text(content.title, color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${content.hostName}发起的群直播",
                    color = Color.White.copy(alpha = .62f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (content.status == "live") "进入" else "已结束",
                    color = Color(0xFF07C160),
                    fontSize = 13.sp
                )
            }
        }
    }
}
