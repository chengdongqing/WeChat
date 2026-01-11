package top.chengdongqing.wechat.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.data.webrtc.WebRtcManager

@Composable
fun CallScreen(
    webRtcManager: WebRtcManager,
    localRenderer: SurfaceViewRenderer,
    remoteRenderer: SurfaceViewRenderer,
    isOfferer: Boolean,
    onHangUp: () -> Unit
) {
    var hasAccepted by remember { mutableStateOf(false) }

    if (!hasAccepted && !isOfferer) {
        // 接收方看到的“待接听”界面
        IncomingCallOverlay(
            peerName = "张三",
            onAccept = {
                hasAccepted = true
                // 这里才真正触发 WebRTC 流程，或者如果之前已经 SetRemoteDescription 了，
                // 这里才允许画面渲染
            },
            onReject = { onHangUp() }
        )
    } else {
        // 视频通话主画面
        FullVideoCallContent(webRtcManager, localRenderer, remoteRenderer, onHangUp)
    }
}

@Composable
private fun FullVideoCallContent(
    webRtcManager: WebRtcManager,
    localRenderer: SurfaceViewRenderer,
    remoteRenderer: SurfaceViewRenderer,
    onHangUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 对方的画面 - 全屏
        WebRtcVideoView(
            eglContext = webRtcManager.eglContext,
            renderer = remoteRenderer,
            modifier = Modifier.fillMaxSize()
        )

        // 自己的画面 - 小窗
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(width = 120.dp, height = 180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
        ) {
            WebRtcVideoView(
                eglContext = webRtcManager.eglContext,
                renderer = localRenderer,
                modifier = Modifier.fillMaxSize(),
                isOverlay = true // 小窗需要置顶
            )
        }

        // 控制按钮
        IconButton(
            onClick = onHangUp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(64.dp)
                .background(Color.Red, CircleShape)
        ) {
            Icon(Icons.Default.CallEnd, contentDescription = "挂断", tint = Color.White)
        }
    }
}

@Composable
private fun WebRtcVideoView(
    eglContext: EglBase.Context, // 必须拿到上下文
    renderer: SurfaceViewRenderer,
    modifier: Modifier,
    isOverlay: Boolean = false
) {
    AndroidView(
        factory = {
            renderer.apply {
                // 初始化渲染环境
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                if (isOverlay) {
                    // 解决两个 SurfaceView 堆叠时，小窗被大窗遮挡的问题
                    setZOrderMediaOverlay(true)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun IncomingCallOverlay(
    peerName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)), // 半透明背景
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. 头像/名称
            Surface(
                shape = CircleShape,
                color = Color.Gray,
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = peerName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Text(
                text = "正在邀请你视频通话...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(64.dp))

            // 2. 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 拒接按钮
                FloatingActionButton(
                    onClick = onReject,
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "拒接")
                }

                // 接听按钮
                FloatingActionButton(
                    onClick = onAccept,
                    containerColor = Color.Green,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "接听")
                }
            }
        }
    }
}