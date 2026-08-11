package top.chengdongqing.wechat.feature.chat.ui.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.feature.chat.service.LiveScreenCaptureService
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun LiveRoomScreen(
    liveId: String,
    isHost: Boolean,
    onBack: () -> Unit,
    viewModel: LiveRoomViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val rootView = LocalView.current
    var micOn by remember { mutableStateOf(true) }
    var cameraOn by remember { mutableStateOf(true) }
    var screenSharing by remember { mutableStateOf(false) }
    var beautyStrength by remember { mutableStateOf(.5f) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val endedExplicitly = remember { mutableStateOf(false) }
    var mediaPermissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        mediaPermissionsGranted = grants[Manifest.permission.CAMERA] == true &&
            grants[Manifest.permission.RECORD_AUDIO] == true
    }
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            screenSharing = true
            LiveScreenCaptureService.start(context, result.resultCode)
            Handler(Looper.getMainLooper()).postDelayed({
                val started = viewModel.webRtcManager.startScreenShare(data) {
                    Handler(Looper.getMainLooper()).post { screenSharing = false }
                }
                if (!started) {
                    screenSharing = false
                    LiveScreenCaptureService.stop(context)
                }
            }, 300)
        }
    }

    LaunchedEffect(isHost) {
        viewModel.enterRoom(isHost)
        if (isHost && !mediaPermissionsGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }
    LaunchedEffect(beautyStrength, isHost) {
        if (isHost) viewModel.webRtcManager.setBeautyStrength(beautyStrength)
    }
    DisposableEffect(isHost) {
        val previousKeepScreenOn = rootView.keepScreenOn
        rootView.keepScreenOn = true
        onDispose {
            rootView.keepScreenOn = previousKeepScreenOn
            if (isHost) {
                viewModel.webRtcManager.stopScreenShare()
                LiveScreenCaptureService.stop(context)
            }
            if (!endedExplicitly.value) viewModel.leaveRoom(isHost)
        }
    }
    BackHandler { showExitConfirm = true }

    Box(Modifier.fillMaxSize().background(Color(0xFF111111))) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isHost) "直播中" else "正在观看直播",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${state.viewerCount}人观看",
                    color = Color.White.copy(alpha = .7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "×",
                    color = Color.White,
                    fontSize = 30.sp,
                    modifier = Modifier.clickable { showExitConfirm = true }
                )
            }

            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!isHost || mediaPermissionsGranted) {
                    LiveWebRtcView(isHost, viewModel.webRtcManager)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painterResource(DesignR.drawable.ic_video_filled),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = .72f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            "请授予相机和麦克风权限",
                            color = Color.White.copy(alpha = .7f),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
                if (screenSharing) {
                    Text(
                        "正在共享屏幕",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .background(Color(0xAA000000), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (isHost) {
                Row(
                    Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    LiveControl(if (micOn) "静音" else "开启麦克风", DesignR.drawable.ic_mic2_filled) {
                        micOn = !micOn
                        viewModel.webRtcManager.setMicEnabled(micOn)
                    }
                    LiveControl(
                        if (cameraOn) "关闭镜头" else "开启镜头",
                        DesignR.drawable.ic_video_filled
                    ) {
                        cameraOn = !cameraOn
                        viewModel.webRtcManager.setCameraEnabled(cameraOn)
                    }
                    LiveControl("翻转镜头", DesignR.drawable.ic_camera_filled) {
                        viewModel.webRtcManager.switchCamera()
                    }
                    LiveControl(
                        if (screenSharing) "停止共享" else "共享屏幕",
                        DesignR.drawable.ic_import_export
                    ) {
                        if (screenSharing) {
                            screenSharing = false
                            viewModel.webRtcManager.stopScreenShare()
                            LiveScreenCaptureService.stop(context)
                        } else {
                            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                                as MediaProjectionManager
                            screenCaptureLauncher.launch(manager.createScreenCaptureIntent())
                        }
                    }
                    LiveControl(
                        when {
                            beautyStrength <= 0f -> "美颜关闭"
                            beautyStrength < .75f -> "美颜自然"
                            else -> "美颜增强"
                        },
                        DesignR.drawable.ic_emoji_outlined
                    ) {
                        beautyStrength = when {
                            beautyStrength <= 0f -> .5f
                            beautyStrength < .75f -> 1f
                            else -> 0f
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(if (isHost) "结束直播？" else "退出直播间？") },
            text = { Text(if (isHost) "确定要结束当前直播吗？" else "确定要离开当前直播吗？") },
            confirmButton = {
                TextButton(onClick = {
                    if (isHost) viewModel.endLive() else viewModel.leaveRoom(false)
                    endedExplicitly.value = true
                    onBack()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("取消") }
            }
        )
    }
    if (!isHost && state.hasEnded) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("直播已结束") },
            text = { Text("主播已结束直播。") },
            confirmButton = { TextButton(onClick = onBack) { Text("退出") } }
        )
    }
}

@Composable
private fun LiveWebRtcView(isHost: Boolean, manager: LiveWebRtcManager) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(manager.eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                setMirror(isHost)
                if (isHost) manager.startHostMedia(this)
                else manager.bindRemoteRenderer(this)
            }
        },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.release() }
    )
}

@Composable
private fun LiveControl(label: String, icon: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            Modifier.size(50.dp).background(Color.White.copy(alpha = .16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(icon),
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(25.dp)
            )
        }
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
    }
}
