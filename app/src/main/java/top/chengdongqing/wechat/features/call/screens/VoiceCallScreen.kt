package top.chengdongqing.wechat.features.call.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import top.chengdongqing.wechat.features.call.CallUiState
import top.chengdongqing.wechat.features.call.components.CallControlBar
import top.chengdongqing.wechat.features.call.components.CallTopBar

/**
 * 语音通话界面
 */
@Composable
fun VoiceCallScreen(
    state: CallUiState,
    onMinimize: () -> Unit,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2C2C2C))
    ) {
        // 背景：模糊的用户头像
        BlurredBackground(
            avatarResId = R.drawable.img_splash
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部栏
            CallTopBar(
                statusText = if (state.isCallActive) state.getStatusText() else "",
                onMinimizeClick = onMinimize
            )

            Spacer(modifier = Modifier.height(100.dp))

            // 用户信息区域
            UserInfoSection(
                userName = state.remoteUser.name,
                statusText = if (!state.isCallActive) state.getStatusText() else null
            )

            Spacer(modifier = Modifier.weight(1f))

            // 底部控制栏
            CallControlBar(
                state = state,
                onAcceptCall = onAcceptCall,
                onRejectCall = onRejectCall,
                onToggleMic = onToggleMic,
                onToggleSpeaker = onToggleSpeaker,
                modifier = Modifier.padding(bottom = 60.dp)
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * 模糊背景
 */
@Composable
private fun BlurredBackground(
    avatarResId: Int,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = avatarResId),
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .blur(50.dp),
        contentScale = ContentScale.Crop,
        alpha = 0.3f
    )
}

/**
 * 用户信息区域
 */
@Composable
private fun UserInfoSection(
    userName: String,
    statusText: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像
        Image(
            painter = painterResource(id = R.drawable.img_avatar),
            contentDescription = userName,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 用户名
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        // 状态文本（非通话中时显示）
        if (statusText != null) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
