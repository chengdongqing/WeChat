package top.chengdongqing.wechat.features.call.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.core.designsystem.util.LaunchedUpdateEffect
import top.chengdongqing.wechat.features.call.domain.model.CallState
import top.chengdongqing.wechat.features.call.ui.components.CallBackground
import top.chengdongqing.wechat.features.call.ui.components.CallControlBar
import top.chengdongqing.wechat.features.call.ui.components.CallTopBar
import top.chengdongqing.wechat.features.call.ui.components.CallUserInfo

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedUpdateEffect(uiState.callState) {
        if (uiState.callState == CallState.Idle) {
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景层
        CallBackground(uiState)
        if (uiState.isVideoCall && uiState.callState != CallState.Ended) {
            if (uiState.isCallActive) {
                WebRtcVideoView(
                    eglContext = viewModel.eglContext,
                    onRendererReady = viewModel::bindRemoteRenderer,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (uiState.isVideoOn) {
                WebRtcVideoView(
                    eglContext = viewModel.eglContext,
                    onRendererReady = viewModel::bindLocalRenderer,
                    isMirror = uiState.isFrontCamera,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 内容层
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶栏
            CallTopBar(
                statusText = if (uiState.isCallActive) uiState.getStatusText() else "",
                onMinimizeClick = viewModel.actions.onMinimize,
                isDarkBackground = uiState.isVideoCall
            )

            Spacer(modifier = Modifier.height(60.dp))

            // 用户信息
            if (!uiState.isVideoCallActive) {
                CallUserInfo(
                    userName = uiState.peerName,
                    userAvatar = uiState.peerAvatar,
                    statusText = if (!uiState.isCallActive) uiState.getStatusText() else null
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部控制栏
            CallControlBar(
                state = uiState,
                actions = viewModel.actions
            )
        }

        // 本地预览小窗
        if (uiState.isVideoCallActive && uiState.isVideoOn) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(20.dp)
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(12.dp))
            ) {
                CallBackground(uiState)
                WebRtcVideoView(
                    eglContext = viewModel.eglContext,
                    onRendererReady = viewModel::bindLocalRenderer,
                    isMirror = uiState.isFrontCamera,
                    isOverlay = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * WebRTC SurfaceViewRenderer 封装
 *
 * 关键三步:
 * 1. renderer.init(eglContext, null)       — 初始化渲染上下文
 * 2. setZOrderMediaOverlay(true)          — 小窗置顶
 * 3. onRendererReady(renderer)            — 绑定到 WebRTCManager 的 track
 */
@Composable
fun WebRtcVideoView(
    eglContext: EglBase.Context,
    onRendererReady: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier,
    isMirror: Boolean = false,
    isOverlay: Boolean = false
) {
    val context = LocalContext.current

    AndroidView(
        factory = {
            SurfaceViewRenderer(context).apply {
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                setMirror(isMirror)
                if (isOverlay) {
                    setZOrderMediaOverlay(true)
                }
                onRendererReady(this)
            }
        },
        update = { view ->
            view.setMirror(isMirror)
        },
        modifier = modifier,
        onRelease = { it.release() }
    )
}