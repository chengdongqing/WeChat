package top.chengdongqing.wechat.features.chat.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SessionAvatar(chat.contactAvatar, chat.unreadCount)
        SessionContent(chat.contactName, chat.lastMessage)
        SessionStatus(chat.lastMessageTime?.toChatDisplayTime(), chat.isMuted)
    }
}

@Composable
private fun SessionAvatar(
    avatar: String?,
    unreadCount: Int
) {
    WeBadge(
        visible = unreadCount > 0,
        content = unreadCount.toBadgeText(),
        size = 20.dp,
        offset = DpOffset(x = 8.dp, y = (-4).dp)
    ) {
        AsyncImage(
            model = avatar,
            contentDescription = null,
            error = painterResource(R.drawable.img_avatar_placeholder),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun RowScope.SessionContent(
    name: String,
    lastMsg: String?
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = name,
            fontSize = 16.sp,
            maxLines = 1,
            color = WeTheme.colorScheme.textPrimary,
            fontWeight = FontWeight.Medium
        )
        lastMsg?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                fontSize = 13.sp,
                color = WeTheme.colorScheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SessionStatus(
    time: String?,
    isMuted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        time?.let {
            Text(
                text = it,
                fontSize = 11.sp,
                color = WeTheme.colorScheme.textSecondary
            )
        }
        if (isMuted) {
            Icon(
                painter = painterResource(R.drawable.ic_mute_outlined),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = WeTheme.colorScheme.textSecondary.copy(alpha = 0.5f)
            )
        } else {
            // 占位，保持右侧布局高度对齐
            Spacer(modifier = Modifier.size(14.dp))
        }
    }
}