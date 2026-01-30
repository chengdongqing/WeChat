package top.chengdongqing.wechat.ui.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.call.model.CallState

@Composable
fun VoiceCallScreen(
    state: CallUiState,
    onAcceptCall: () -> Unit,
    onHangup: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C2C2C))
    ) {
        // 1. 背景高斯模糊 (使用对方头像)
        // 这里模拟一个头像，实际开发中 state 应包含 avatarUrl
        Image(
            painter = painterResource(id = R.drawable.img_avatar),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(50.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // 2. 用户信息区
            Image(
                painter = painterResource(id = R.drawable.img_avatar),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = state.remoteUserName,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 状态提示或计时
            // 根据 state.callState 判断显示内容
            val statusText = when (state.callState) {
                is CallState.Connecting -> "正在呼叫..."
                is CallState.Ringing -> "等待对方接听..."
                is CallState.Active -> state.durationText
                is CallState.Ended -> "通话已结束"
                else -> ""
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 4. 底部控制区
            CallControls(
                state = state,
                onAcceptCall = onAcceptCall,
                onHangup = onHangup,
                onToggleMic = onToggleMic,
                onToggleSpeaker = onToggleSpeaker
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}