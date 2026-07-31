package top.chengdongqing.wechat.feature.discovery.intercom

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercomRoomScreen(
    channel: String,
    onBack: () -> Unit,
    viewModel: IntercomViewModel = hiltViewModel()
) {
    var isTalking by remember { mutableStateOf(false) }
    var speakerEnabled by remember { mutableStateOf(true) }
    var showMembers by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var microphoneDenied by remember { mutableStateOf(false) }
    val roomState by viewModel.roomState.collectAsStateWithLifecycle()
    val activeConnectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        microphoneDenied = !granted
    }
    DisposableEffect(channel) {
        viewModel.enterRoom(channel)
        onDispose {
            // Releasing push-to-talk is screen-scoped, but listening is owned by the
            // foreground service and must survive lock-screen/activity destruction.
            viewModel.setSpeaking(false)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF08110D), Color(0xFF10241A), Color(0xFF07100C))
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            WeTopAppBar(
                title = roomState.channelName.takeIf { it.isNotBlank() }
                    ?.let { "$it · #$channel" }
                    ?: "频道 #$channel",
                onBack = onBack,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            painterResource(R.drawable.ic_more_outlined),
                            "频道设置",
                            tint = Color.White
                        )
                    }
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = .07f))
                        .clickable { showMembers = true }
                        .padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(Color(0xFF43D17A), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${roomState.members.size} 人在线",
                        color = Color.White.copy(alpha = .75f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painterResource(R.drawable.ic_right_outlined),
                        null,
                        tint = Color.White.copy(alpha = .45f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(Modifier.height(38.dp))
                SpeakingStage(isTalking = isTalking, speakers = roomState.speakers)
                Spacer(Modifier.weight(1f))
                Text(
                    when {
                        isTalking -> "正在发送你的声音…"
                        roomState.speakers.isEmpty() -> "频道当前安静"
                        else -> "${roomState.speakers.size} 人正在讲话"
                    },
                    color = if (isTalking) Color(0xFF79E7A7) else Color.White.copy(alpha = .68f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(18.dp))
                PushToTalkButton(
                    isTalking = isTalking,
                    onTalkingChanged = {
                        when {
                            it && !viewModel.canRecord() -> {
                                isTalking = false
                                microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                            }

                            else -> {
                                isTalking = viewModel.setSpeaking(it) && it
                                if (it) microphoneDenied = false
                            }
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    if (microphoneDenied) "需要麦克风权限才能讲话" else "按住讲话 · 松开发送",
                    color = if (microphoneDenied) Color(0xFFFF8A80) else Color.White.copy(alpha = .42f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(28.dp))
                RoomControls(
                    speakerEnabled = speakerEnabled,
                    onSpeakerToggle = {
                        speakerEnabled = !speakerEnabled
                        viewModel.setPlaybackEnabled(speakerEnabled)
                    },
                    onMembers = { showMembers = true },
                    connectionLabel = activeConnectionMode.shortLabel()
                )
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(20.dp)
                )
            }
        }
    }

    if (showMembers) {
        MembersSheet(members = roomState.members, onDismiss = { showMembers = false })
    }
    if (showSettings) {
        ChannelSettingsDialog(
            roomState = roomState,
            connectionMode = activeConnectionMode,
            onDismiss = { showSettings = false },
            onLeave = {
                showSettings = false
                viewModel.leave()
                onBack()
            }
        )
    }
}

@Composable
private fun ChannelSettingsDialog(
    roomState: IntercomRoomState,
    connectionMode: ConnectionMode,
    onDismiss: () -> Unit,
    onLeave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("频道设置") },
        text = {
            Column {
                Text(
                    "频道：${roomState.channelName}",
                    color = WeTheme.colorScheme.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "在线成员：${roomState.members.size} 人",
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "连接方式：${connectionMode.fullLabel()}",
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
                Text(
                    "语音格式：16 kHz Opus · 24 kbps · 低延迟",
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
                Text(
                    "多人模式：本机实时混音",
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
                Text(
                    if (connectionMode == ConnectionMode.WiFiLan) {
                        "安全状态：局域网广播当前未加密"
                    } else {
                        "安全状态：跟随当前点对点连接的端到端加密设置"
                    },
                    color = if (connectionMode == ConnectionMode.WiFiLan) WeTheme.colorScheme.danger else WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
        dismissButton = {
            TextButton(onClick = onLeave) {
                Text(
                    "退出频道",
                    color = WeTheme.colorScheme.danger
                )
            }
        }
    )
}

private fun ConnectionMode.shortLabel() = when (this) {
    ConnectionMode.WiFiLan -> "局域网"
    ConnectionMode.WiFiDirect -> "直连"
    ConnectionMode.Bluetooth -> "蓝牙"
}

private fun ConnectionMode.fullLabel() = when (this) {
    ConnectionMode.WiFiLan -> "Wi‑Fi 局域网"
    ConnectionMode.WiFiDirect -> "Wi‑Fi Direct"
    ConnectionMode.Bluetooth -> "蓝牙 RFCOMM"
}
