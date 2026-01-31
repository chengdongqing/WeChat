package top.chengdongqing.wechat.ui.chat.session.message.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun UserCardContent(content: MessageContent.UserCard) {
    Column(
        modifier = Modifier
            .clickable {}
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = R.drawable.img_avatar_placeholder,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = content.name,
                fontSize = 16.sp,
                color = WeChatTheme.colorScheme.textPrimary
            )
        }
        WeDivider(modifier = Modifier.padding(top = 12.dp))
        Text(
            text = "个人名片",
            fontSize = 10.sp,
            color = WeChatTheme.colorScheme.textSecondary,
            modifier = Modifier.offset(y = 5.dp)
        )
    }
}