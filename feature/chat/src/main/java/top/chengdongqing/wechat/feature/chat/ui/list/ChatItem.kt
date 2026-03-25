package top.chengdongqing.wechat.feature.chat.ui.list

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.common.util.toChatDisplayTime
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.ui.toPreviewText
import top.chengdongqing.wechat.core.designsystem.util.RichTextMode
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.designsystem.util.parseRichText
import top.chengdongqing.wechat.core.designsystem.util.rememberEmojiInlineContent
import top.chengdongqing.wechat.core.model.ChatSession
import top.chengdongqing.wechat.core.model.MessageType

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
private fun RowScope.SessionContent(session: ChatSession) {
    val annotatedText = rememberAnnotatedText(session)
    val inlineContent = rememberEmojiInlineContent(annotatedText, emojiSize = 17.sp)
    val normalColor = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = session.contactName,
            fontSize = 16.sp,
            maxLines = 1,
            color = WeTheme.colorScheme.textPrimary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (session.isSending) {
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
    val resources = LocalResources.current
    val lastMessageTime = chat.lastMessageTime?.toChatDisplayTime(resources) ?: ""

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

@Composable
private fun rememberAnnotatedText(session: ChatSession): AnnotatedString {
    val context = LocalContext.current
    val resources = LocalResources.current
    val isDraft = session.draftMessage?.isNotBlank().isTrue()
    val draftColor = WeTheme.colorScheme.danger

    return remember(session) {
        buildAnnotatedString {
            when {
                // 提示已撤回
                session.lastMessageRecalled && !isDraft -> {
                    val resId = if (session.lastMessageFromMe) {
                        R.string.chat_recalled_by_me
                    } else {
                        R.string.chat_recalled_by_other
                    }
                    append(resources.getString(resId))
                }

                // 文本消息显示内容
                session.lastMessageType == MessageType.Text || isDraft -> {
                    val bodyText =
                        if (isDraft) session.draftMessage!! else session.lastMessage ?: ""
                    val parsedBody = bodyText.parseRichText(mode = RichTextMode.EmojiOnly)

                    // 拼接草稿前缀 + 正文；仅正文部分走表情解析
                    if (isDraft) {
                        withStyle(SpanStyle(color = draftColor)) {
                            append(resources.getString(R.string.message_preview_draft) + " ")
                        }
                    }
                    append(parsedBody)
                }

                // 其他消息显示类型名称
                else -> {
                    session.lastMessageType?.toPreviewText(context, "")?.let { append(it) }
                }
            }
        }
    }
}