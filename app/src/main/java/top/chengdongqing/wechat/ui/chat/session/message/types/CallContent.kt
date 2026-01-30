package top.chengdongqing.wechat.ui.chat.session.message.types

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.call.model.CallType
import top.chengdongqing.wechat.data.model.CallStatus
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.data.model.MessageContent

@Composable
fun CallContent(message: ChatMessage) {
    val isFromMe = message.isFromMe
    val content = message.content as MessageContent.Call
    val isVideoCall = content.type == CallType.VIDEO
    val status = content.status

    val description = remember {
        if (status == CallStatus.CONNECTED) {
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
                painter = painterResource(if (isVideoCall) R.drawable.ic_video_call_outlined else R.drawable.ic_voice_call_outlined),
                contentDescription = if (isVideoCall) "视频通话" else "语音通话",
                Modifier
                    .size(22.dp)
                    .graphicsLayer(scaleX = if (isFromMe && isVideoCall) -1f else 1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                )
            )
        }
    }
}