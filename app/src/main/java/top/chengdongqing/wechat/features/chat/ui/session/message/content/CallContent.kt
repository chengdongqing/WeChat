package top.chengdongqing.wechat.features.chat.ui.session.message.content

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.call.model.CallStatus
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

@Composable
fun CallContent(message: ChatMessage) {
    val isFromMe = message.isFromMe
    val content = message.content as MessageContent.Call
    val isVideoCall = content.type.isVideoCall
    val status = content.status

    val description = remember {
        if (status == CallStatus.Finished) {
            content.duration?.let {
                return@remember CallStatus.describeDuration(it)
            }
        }
        if (isFromMe) status.descriptionForMe else status.description
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (isFromMe) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (isVideoCall) R.drawable.ic_video_outlined else R.drawable.ic_hangup_outlined),
                contentDescription = content.type.label,
                tint = WeTheme.colorScheme.textPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer(scaleX = if (isFromMe && isVideoCall) -1f else 1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 16.sp,
                    color = WeTheme.colorScheme.textPrimary
                )
            )
        }
    }
}