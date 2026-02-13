package top.chengdongqing.wechat.features.chat.ui.list

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.util.toChatDisplayTime
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession

@Composable
fun ChatItem(chat: ChatSession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeBadge(
            visible = chat.unreadCount > 0,
            content = chat.unreadCount.toBadgeText(),
            size = 20.dp,
            offset = DpOffset(x = 8.dp, y = (-4).dp)
        ) {
            AsyncImage(
                model = chat.contactAvatar,
                contentDescription = null,
                error = painterResource(R.drawable.img_avatar_placeholder),
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.contactName,
                fontSize = 16.sp,
                color = WeTheme.colorScheme.textPrimary,
                fontWeight = FontWeight.Medium
            )
            chat.lastMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chat.lastMessage,
                    fontSize = 13.sp,
                    color = WeTheme.colorScheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = chat.lastMessageTime.toChatDisplayTime(),
                fontSize = 12.sp,
                color = WeTheme.colorScheme.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_mute_outlined),
                contentDescription = "已开启免打扰",
                modifier = Modifier
                    .size(16.dp)
                    .alpha(if (chat.isMuted) 1f else 0f),
                tint = WeTheme.colorScheme.textSecondary
            )
        }
    }
}