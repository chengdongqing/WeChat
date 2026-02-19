package top.chengdongqing.wechat.features.call.ui

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.core.designsystem.util.ImmersiveSystemBars
import top.chengdongqing.wechat.core.designsystem.util.StatusBarAppearanceEffect
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.features.call.domain.model.CallState
import top.chengdongqing.wechat.features.call.domain.model.CallUiState
import top.chengdongqing.wechat.features.call.ui.components.CallBackground
import top.chengdongqing.wechat.features.call.ui.components.CallControlBar
import top.chengdongqing.wechat.features.call.ui.components.CallTopBar
import top.chengdongqing.wechat.features.call.ui.components.CallUserInfo
import kotlin.math.roundToInt

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // 通话结束 → 自动退出
    LaunchedEffect(uiState.callState) {
        if (uiState.callState == CallState.Ended) {
            delay(2000)
            onDismiss()

            // 重置状态（因为CallManager是单例）
            delay(200)
            viewModel.resetState()
        }
    }

    // 沉浸式状态栏
    ImmersiveSystemBars(uiState.isControlsVisible)
    StatusBarAppearanceEffect(isDark = false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .weClickable(enabled = uiState.canToggleControls) {
                viewModel.actions.onToggleControlsVisibility()
            }
    ) {
        // 全屏视频
        FullScreenLayer(uiState, viewModel)

        // 控件
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.fillMaxSize()
        ) {
            ControlsLayer(uiState, viewModel)
        }

        // 画中画小窗
        if (uiState.showFloatingWindow) {
            FloatingPipWindow(uiState, viewModel)
        }
    }
}

@Composable
private fun FullScreenLayer(uiState: CallUiState, viewModel: CallViewModel) {
    when {
        // 视频通话中：显示大画面
        uiState.showRemoteVideo -> {
            val isSwapped = uiState.isVideoSwapped
            WebRtcVideoView(
                eglContext = viewModel.eglContext,
                onRendererReady = viewModel::bindRemoteRenderer,
                // 本地画面 + 前置摄像头 → 镜像
                isMirror = isSwapped && uiState.isFrontCamera
            )
        }

        // 呼出/连接中：全屏本地预览
        uiState.showFullScreenLocalPreview -> {
            WebRtcVideoView(
                eglContext = viewModel.eglContext,
                onRendererReady = viewModel::bindLocalRenderer,
                isMirror = uiState.isFrontCamera
            )
        }

        // 语音通话 / 对方关闭摄像头 → 模糊背景
        else -> CallBackground(uiState)
    }
}

@Composable
private fun ControlsLayer(uiState: CallUiState, viewModel: CallViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶栏：通话时长 + 最小化按钮
        CallTopBar(
            statusText = if (uiState.isCallActive) uiState.getStatusText() else "",
            onMinimizeClick = viewModel.actions.onMinimize,
            isDarkBackground = uiState.isVideoCall
        )

        // 中间区域：头像 + 状态文字
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = if (uiState.showCenterAvatar) 0.dp else 60.dp),
            contentAlignment =
                if (uiState.showCenterAvatar) Alignment.Center else Alignment.TopCenter
        ) {
            if (uiState.showTopUserInfo) {
                CallUserInfo(
                    userName = uiState.peerName,
                    userAvatar = uiState.peerAvatar,
                    largeAvatar = uiState.showCenterAvatar,
                    statusText = if (!uiState.isCallActive) uiState.getStatusText() else null
                )
            }
        }

        // 底部控制栏
        CallControlBar(state = uiState, actions = viewModel.actions)
    }
}

@Composable
private fun FloatingPipWindow(uiState: CallUiState, viewModel: CallViewModel) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize

    // 屏幕尺寸 (px)
    val screenW = containerSize.width.toFloat()
    val screenH = containerSize.height.toFloat()

    // 小窗尺寸 (px)
    val pipW = with(density) { PIP_WIDTH.toPx() }
    val pipH = with(density) { PIP_HEIGHT.toPx() }
    val margin = with(density) { PIP_MARGIN.toPx() }

    // 状态栏安全区
    val statusBarH = with(density) { STATUS_BAR_HEIGHT.toPx() }

    // 拖拽偏移量（初始位置：右上角，避开状态栏）
    val offsetXAnim = remember { Animatable(screenW - pipW - margin) }
    val offsetYAnim = remember { Animatable(statusBarH + margin) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetXAnim.value.roundToInt(), offsetYAnim.value.roundToInt()) }
            .size(PIP_WIDTH, PIP_HEIGHT)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // 松手：吸附到最近边缘
                        val centerX = offsetXAnim.value + pipW / 2
                        val targetX = if (centerX < screenW / 2) margin
                        else screenW - pipW - margin

                        scope.launch {
                            offsetXAnim.animateTo(
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
                        offsetXAnim.snapTo(
                            (offsetXAnim.value + dragAmount.x).coerceIn(0f, screenW - pipW)
                        )
                        offsetYAnim.snapTo(
                            (offsetYAnim.value + dragAmount.y).coerceIn(statusBarH, screenH - pipH)
                        )
                    }
                }
            }
            .weClickable { viewModel.actions.onSwapVideo() }
    ) {
        val isSwapped = uiState.isVideoSwapped
        WebRtcVideoView(
            eglContext = viewModel.eglContext,
            onRendererReady = viewModel::bindLocalRenderer,
            isMirror = !isSwapped && uiState.isFrontCamera,
            isOverlay = true
        )
    }
}

// 小窗尺寸常量
private val PIP_WIDTH = 110.dp
private val PIP_HEIGHT = 160.dp
private val PIP_MARGIN = 16.dp
private val STATUS_BAR_HEIGHT = 48.dp

/**
 * WebRTC 视频渲染组件
 *
 * @param eglContext      共享 EGL 上下文
 * @param onRendererReady 初始化完成后回调，将 renderer 绑定到 WebRTC 视频轨道
 * @param isMirror        前置摄像头镜像显示
 * @param isOverlay       小窗置顶（防止被全屏 SurfaceView 遮挡）
 */
@Composable
fun WebRtcVideoView(
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
                if (isOverlay) {
                    setZOrderMediaOverlay(true)
                }
                onRendererReady(this)
            }
        },
        update = { view ->
            view.setMirror(isMirror)
        },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.release() }
    )
}