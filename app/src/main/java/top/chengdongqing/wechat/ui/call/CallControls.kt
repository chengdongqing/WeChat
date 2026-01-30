package top.chengdongqing.wechat.ui.call

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.call.model.CallState


@Composable
fun CallControls(
    state: CallUiState,
    onHangup: () -> Unit,
    onAcceptCall: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.callState is CallState.Active) {
            // 通话中的按钮布局
            CircularControlButton(
                icon = if (state.isMuted) R.drawable.ic_mic_off_filled else R.drawable.ic_mic_filled,
                text = "静音",
                onClick = onToggleMic,
                active = state.isMuted
            )

            CircularControlButton(
                icon = R.drawable.ic_hangup_filled,
                text = "挂断",
                backgroundColor = Color.Red,
                onClick = onHangup
            )

            CircularControlButton(
                icon = if (state.isSpeakerOn) R.drawable.ic_speaker_filled else R.drawable.ic_speaker_off_filled,
                text = "扬声器",
                onClick = onToggleSpeaker,
                active = state.isSpeakerOn
            )
        } else {
            // 呼叫中状态 (简单示例：只显示一个取消/挂断)
            CircularControlButton(
                icon = R.drawable.ic_hangup_filled,
                text = "取消",
                backgroundColor = Color.Red,
                onClick = onHangup
            )

            // 如果是被叫，这里应该还有一个接听按钮
            // if (isIncomingCall) { ... }
        }
    }
}

@Composable
fun CircularControlButton(
    @DrawableRes icon: Int,
    text: String,
    backgroundColor: Color = Color.White.copy(alpha = 0.2f),
    active: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(if (active) Color.White else backgroundColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = if (active) Color.Black else Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}