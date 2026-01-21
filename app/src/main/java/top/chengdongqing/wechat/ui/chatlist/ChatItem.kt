package top.chengdongqing.wechat.ui.chatlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.Chat
import top.chengdongqing.wechat.ui.components.badge.WeBadge
import top.chengdongqing.wechat.ui.components.badge.toBadgeText
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun ChatItem(chat: Chat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeBadge(
            content = chat.unreadCount.toBadgeText(),
            size = 20.dp,
            offset = DpOffset(x = 8.dp, y = (-4).dp)
        ) {
            Image(
                painter = painterResource(R.drawable.img_avatar),
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                fontSize = 16.sp,
                color = WeChatTheme.colorScheme.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                fontSize = 13.sp,
                color = WeChatTheme.colorScheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = chat.time,
                fontSize = 12.sp,
                color = WeChatTheme.colorScheme.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_bellring_off),
                contentDescription = "已开启免打扰",
                modifier = Modifier.size(16.dp),
                tint = WeChatTheme.colorScheme.textSecondary
            )
        }
    }
}