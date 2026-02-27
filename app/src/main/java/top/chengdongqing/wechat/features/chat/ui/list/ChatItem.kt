package top.chengdongqing.wechat.features.chat.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
        SessionAvatar(
            avatar = chat.contactAvatar,
            unreadCount = chat.unreadCount
        )
        SessionContent(
            name = chat.contactName,
            lastMsg = chat.lastMessage,
            isSending = chat.isSending
        )
        SessionStatus(
            time = chat.lastMessageTime?.toChatDisplayTime(),
            isSending = chat.isSending,
            isMuted = chat.isMuted
        )
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
    lastMsg: String?,
    isSending: Boolean
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
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSending) {
                Icon(
                    painter = painterResource(R.drawable.ic_sending_filled),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(y = 1.dp),
                    tint = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f)
                )
            }
            Text(
                text = lastMsg ?: "",
                fontSize = 13.sp,
                color = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(if (lastMsg == null) 0f else 1f)
            )
        }
    }
}

@Composable
private fun SessionStatus(
    time: String?,
    isSending: Boolean,
    isMuted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isSending) "正在发送中" else time ?: "",
            fontSize = 11.sp,
            color = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_mute_outlined),
            contentDescription = null,
            modifier = Modifier
                .size(14.dp)
                .alpha(if (isMuted) 1f else 0f),
            tint = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f)
        )
    }
}