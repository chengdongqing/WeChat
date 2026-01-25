package top.chengdongqing.wechat.ui.chat.session.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.ChatMessage

@Composable
fun MessageItem(
    message: ChatMessage,
    avatarRes: Int = R.drawable.img_avatar
) {
    val isFromMe = message.isFromMe

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromMe) {
            Avatar(avatarRes)
            Spacer(modifier = Modifier.width(12.dp))
        }

        ChatBubble(isFromMe = isFromMe, showArrow = true) {
            MessageContent(message.content)
        }

        if (isFromMe) {
            Spacer(modifier = Modifier.width(12.dp))
            Avatar(avatarRes)
        }
    }
}

@Composable
private fun Avatar(resId: Int) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}