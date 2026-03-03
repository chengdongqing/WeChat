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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.RichTextMode
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.designsystem.util.parseRichText
import top.chengdongqing.wechat.core.designsystem.util.rememberEmojiInlineContent
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
        SessionAvatar(chat)
        SessionContent(chat)
        SessionStatus(chat)
    }
}

@Composable
private fun SessionAvatar(chat: ChatSession) {
    val badgeContent = if (!chat.isMuted) chat.unreadCount.toBadgeText() else null
    val badgeSize = if (!chat.isMuted) 20.dp else 10.dp
    val badgeOffset = DpOffset(x = if (!chat.isMuted) 8.dp else 4.dp, y = (-4).dp)

    WeBadge(
        visible = chat.unreadCount > 0,
        content = badgeContent,
        size = badgeSize,
        offset = badgeOffset
    ) {
        AsyncImage(
            model = chat.contactAvatar,
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
private fun RowScope.SessionContent(chat: ChatSession) {
    val isDraft = chat.draftMessage?.isNotBlank().isTrue()
    val draftColor = WeTheme.colorScheme.error
    val normalColor = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f)

    // 拼接草稿前缀 + 正文；仅正文部分走表情解析
    val annotatedText = remember(isDraft, chat.draftMessage, chat.lastMessage) {
        val bodyText = if (isDraft) chat.draftMessage!! else chat.lastMessage ?: ""
        val parsedBody = bodyText.parseRichText(mode = RichTextMode.EmojiOnly)

        buildAnnotatedString {
            if (isDraft) {
                withStyle(SpanStyle(color = draftColor)) { append("[草稿] ") }
            }
            append(parsedBody)
        }
    }

    val inlineContent = rememberEmojiInlineContent(annotatedText, emojiSize = 17.sp)

    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = chat.contactName,
            fontSize = 16.sp,
            maxLines = 1,
            color = WeTheme.colorScheme.textPrimary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (chat.isSending) {
                Icon(
                    painter = painterResource(R.drawable.ic_sending_filled),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(y = 1.dp),
                    tint = normalColor,
                )
            }
            Text(
                text = annotatedText,
                inlineContent = inlineContent,
                fontSize = 13.sp,
                color = normalColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionStatus(chat: ChatSession) {
    val lastMessageTime = chat.lastMessageTime?.toChatDisplayTime() ?: ""

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (chat.isSending) "正在发送中" else lastMessageTime,
            fontSize = 11.sp,
            color = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_mute_outlined),
            contentDescription = null,
            modifier = Modifier
                .size(14.dp)
                .alpha(if (chat.isMuted) 1f else 0f),
            tint = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f)
        )
    }
}