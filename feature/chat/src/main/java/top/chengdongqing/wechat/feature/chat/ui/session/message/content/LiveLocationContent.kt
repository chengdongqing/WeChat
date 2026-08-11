package top.chengdongqing.wechat.feature.chat.ui.session.message.content

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun LiveLocationContent(message: ChatMessage) {
    val content = message.content as MessageContent.LiveLocation
    val active = LocalChatSessionContext.current?.activeLiveLocationRoomId == content.roomId
    Row(
        modifier = Modifier
            .width(210.dp)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(DesignR.drawable.ic_location_filled),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (active) Color(0xFF07C160) else Color.Gray
        )
        Text(
            text = if (!active) stringResource(R.string.live_location_ended)
            else if (message.isFromMe) stringResource(R.string.live_location_started_me)
            else stringResource(DesignR.string.live_location_started_peer),
            modifier = Modifier.padding(start = 9.dp),
            color = if (active) Color.Unspecified else Color.Gray
        )
    }
}
