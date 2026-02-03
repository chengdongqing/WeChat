package top.chengdongqing.wechat.features.call.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.features.call.CallUiState
import top.chengdongqing.wechat.features.call.components.CallControlBar
import top.chengdongqing.wechat.features.call.components.CallTopBar

/**
 * 视频通话界面
 */
@Composable
fun VideoCallScreen(
    state: CallUiState,
    onMinimize: () -> Unit,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 底层：远端视频流或背景
        RemoteVideoLayer(
            isCallActive = state.isCallActive,
            shouldShowRemoteVideo = state.shouldShowRemoteVideo
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            CallTopBar(
                statusText = if (state.isCallActive) state.getStatusText() else "",
                onMinimizeClick = onMinimize,
                isDarkBackground = true
            )

            // 非通话状态显示用户信息
            if (!state.isCallActive) {
                Spacer(modifier = Modifier.height(60.dp))
                VideoCallUserInfo(
                    userName = state.remoteUser.name,
                    statusText = state.getStatusText()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 本地预览窗口
            if (state.shouldShowLocalPreview) {
                LocalPreviewWindow(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                )
            }

            // 底部控制栏
            CallControlBar(
                state = state,
                onAcceptCall = onAcceptCall,
                onRejectCall = onRejectCall,
                onToggleMic = onToggleMic,
                onToggleSpeaker = onToggleSpeaker,
                onSwitchCamera = onSwitchCamera,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * 远端视频层
 */
@Composable
private fun RemoteVideoLayer(
    isCallActive: Boolean,
    shouldShowRemoteVideo: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 通话中且有视频流
            shouldShowRemoteVideo -> {
                RemoteVideoView()
            }
            // 通话中但无视频流
            isCallActive -> {
                PlaceholderVideoView()
            }
            // 未接通显示模糊背景
            else -> {
                BlurredBackgroundView()
            }
        }
    }
}

/**
 * 远端视频视图（实际开发中使用SurfaceView）
 */
@Composable
private fun RemoteVideoView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        // TODO: 实际渲染远端视频流
        Text("对方视频流", color = Color.Gray)
    }
}

/**
 * 占位视图
 */
@Composable
private fun PlaceholderVideoView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Text("等待对方开启视频...", color = Color.Gray)
    }
}

/**
 * 模糊背景视图
 */
@Composable
private fun BlurredBackgroundView() {
    Image(
        painter = painterResource(id = R.drawable.img_splash),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .blur(60.dp),
        contentScale = ContentScale.Crop,
        alpha = 0.4f
    )
}

/**
 * 视频通话用户信息
 */
@Composable
private fun VideoCallUserInfo(
    userName: String,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像
        Image(
            painter = painterResource(id = R.drawable.img_avatar),
            contentDescription = userName,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 用户名
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 状态文本
        Text(
            text = statusText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}

/**
 * 本地预览窗口
 */
@Composable
private fun LocalPreviewWindow(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 110.dp, height = 160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // TODO: 实际渲染本地摄像头预览
        Text(
            text = "我",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            color = Color.White,
            fontSize = 10.sp
        )
    }
}
