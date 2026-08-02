package top.chengdongqing.wechat.feature.call.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.window.ImmersiveSystemBars
import top.chengdongqing.wechat.core.designsystem.window.StatusBarAppearanceEffect
import top.chengdongqing.wechat.core.model.CallState
import top.chengdongqing.wechat.feature.call.domain.model.CallUiState
import kotlin.math.roundToInt

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onDismiss: () -> Unit,
    onMinimize: () -> Unit
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // 通话结束后延迟 2s 退出，给用户看到结束状态
    LaunchedEffect(uiState.callState) {
        if (uiState.callState == CallState.Ended) {
            delay(2000)
            onDismiss()
        }
    }

    LifecycleResumeEffect(Unit) {
        // 刚进入接听页面有时会黑屏，可能需要重新采集摄像头数据
        if (uiState.callState == CallState.Incoming && uiState.isVideoCall && uiState.isVideoOn) {
            viewModel.restartVideoCapture()
        }
        onPauseOrDispose {}
    }

    ImmersiveSystemBars(!uiState.isControlsVisible)
    StatusBarAppearanceEffect(isDark = false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onTap(enabled = uiState.canToggleControls) {
                viewModel.actions.onToggleControlsVisibility()
            }
    ) {
        FullScreenLayer(uiState, viewModel)

        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.fillMaxSize()
        ) {
            ControlsLayer(uiState, viewModel, onMinimize)
        }

        if (uiState.showFloatingWindow) {
            FloatingPipWindow(uiState, viewModel)
        }
    }
}

/**
 * 全屏背景层
 *
 * 视频通话中显示远端（或本端）画面；非视频或对方关闭摄像头时显示背景。
 */
@Composable
private fun FullScreenLayer(uiState: CallUiState, viewModel: CallViewModel) {
    when {
        uiState.showRemoteVideo -> WebRTCVideoView(
            eglContext = viewModel.eglContext,
            onRendererReady = viewModel::bindRemoteRenderer,
            // 交换后显示本地画面，前置摄像头需镜像
            isMirror = uiState.isVideoSwapped && uiState.isFrontCamera
        )

        uiState.showFullScreenLocalPreview -> WebRTCVideoView(
            eglContext = viewModel.eglContext,
            onRendererReady = viewModel::bindLocalRenderer,
            isMirror = uiState.isFrontCamera
        )

        else -> CallBackground(uiState)
    }
}

/**
 * 控件层：顶栏 + 中间用户信息 + 底部操作栏
 */
@Composable
private fun ControlsLayer(
    uiState: CallUiState,
    viewModel: CallViewModel,
    onMinimize: () -> Unit
) {
    val context = LocalContext.current
    val statusText = uiState.getStatusText(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CallTopBar(
            statusText = if (uiState.isCallActive) statusText else null,
            onMinimizeClick = onMinimize,
            isDarkBackground = uiState.isVideoCall
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = if (uiState.showCenterAvatar) 0.dp else 60.dp),
            contentAlignment = if (uiState.showCenterAvatar) Alignment.Center else Alignment.TopCenter
        ) {
            if (uiState.showTopUserInfo) {
                CallUserInfo(
                    userName = uiState.peerName,
                    userAvatar = uiState.peerAvatar,
                    largeAvatar = uiState.showCenterAvatar,
                    statusText = if (!uiState.isCallActive) statusText else null
                )
            }
        }

        CallControlBar(state = uiState, actions = viewModel.actions)
    }
}

/**
 * 画中画小窗（本端预览）
 *
 * 可拖拽，松手时吸附到左右边缘
 */
@Composable
private fun FloatingPipWindow(uiState: CallUiState, viewModel: CallViewModel) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize

    val screenW = containerSize.width.toFloat()
    val screenH = containerSize.height.toFloat()
    val pipW = with(density) { PIP_WIDTH.toPx() }
    val pipH = with(density) { PIP_HEIGHT.toPx() }
    val margin = with(density) { PIP_MARGIN.toPx() }
    val statusBarH = with(density) { STATUS_BAR_HEIGHT.toPx() }

    // 初始位置：右上角，避开状态栏
    val offsetX = remember { Animatable(screenW - pipW - margin) }
    val offsetY = remember { Animatable(statusBarH + margin) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .size(PIP_WIDTH, PIP_HEIGHT)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // 松手时吸附到最近的左/右边缘
                        val targetX = if (offsetX.value + pipW / 2 < screenW / 2) margin
                        else screenW - pipW - margin
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = targetX,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        offsetX.snapTo((offsetX.value + dragAmount.x).coerceIn(0f, screenW - pipW))
                        offsetY.snapTo(
                            (offsetY.value + dragAmount.y).coerceIn(
                                statusBarH,
                                screenH - pipH
                            )
                        )
                    }
                }
            }
            .onTap { viewModel.actions.onSwapVideo() }
    ) {
        WebRTCVideoView(
            eglContext = viewModel.eglContext,
            onRendererReady = viewModel::bindLocalRenderer,
            // 未交换时小窗显示本端，前置摄像头需镜像
            isMirror = !uiState.isVideoSwapped && uiState.isFrontCamera,
            isOverlay = true
        )
    }
}

/**
 * WebRTC 视频渲染组件
 */
@Composable
private fun WebRTCVideoView(
    eglContext: EglBase.Context,
    onRendererReady: (SurfaceViewRenderer) -> Unit,
    isMirror: Boolean = false,
    isOverlay: Boolean = false
) {
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                setMirror(isMirror)
                if (isOverlay) setZOrderMediaOverlay(true)
                onRendererReady(this)
            }
        },
        update = { it.setMirror(isMirror) },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.release() }
    )
}

// 画中画小窗尺寸
private val PIP_WIDTH = 110.dp
private val PIP_HEIGHT = 160.dp
private val PIP_MARGIN = 16.dp
private val STATUS_BAR_HEIGHT = 48.dp
