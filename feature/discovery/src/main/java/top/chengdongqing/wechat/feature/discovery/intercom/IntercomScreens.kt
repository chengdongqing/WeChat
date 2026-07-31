package top.chengdongqing.wechat.feature.discovery.intercom

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun IntercomLobbyScreen(
    onBack: () -> Unit,
    onJoinChannel: (String) -> Unit,
    viewModel: IntercomViewModel = hiltViewModel()
) {
    var channel by remember { mutableStateOf("5200") }
    var showCreateDialog by remember { mutableStateOf(false) }
    val nearbyChannels by viewModel.channels.collectAsStateWithLifecycle()
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val transportAvailable = true
    val joinChannel: (String, String) -> Unit = { id, name ->
        if (transportAvailable) {
            viewModel.join(id, name)
            onJoinChannel(id)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.background)
    ) {
        WeTopAppBar(
            title = "语音对讲",
            onBack = onBack,
            actions = {
                IconButton(
                    enabled = transportAvailable,
                    onClick = { showCreateDialog = true }
                ) {
                    Icon(painterResource(R.drawable.ic_plus_outlined), "创建频道")
                }
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LobbyHero()
            SectionLabel("当前连接")
            CurrentConnectionCard(connectionMode)
            SectionLabel("快速加入")
            JoinCard(
                channel = channel,
                enabled = transportAvailable,
                onChannelChanged = { channel = it.filter(Char::isDigit).take(6) },
                onJoin = { if (channel.isNotBlank()) joinChannel(channel, "频道 $channel") }
            )
            SectionLabel("附近频道")
            if (nearbyChannels.isEmpty()) {
                EmptyNearbyChannels()
            } else {
                nearbyChannels.forEach { nearby ->
                    NearbyChannelCard(
                        channel = nearby.id,
                        title = nearby.name,
                        members = nearby.memberCount,
                        activity = if (nearby.speakingCount == 0) "安静" else "${nearby.speakingCount} 人正在讲话",
                        onClick = { joinChannel(nearby.id, nearby.name) }
                    )
                }
            }
            Text(
                text = "频道仅对当前网络中的设备可见，不会上传语音内容。",
                color = WeTheme.colorScheme.textTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    if (showCreateDialog) {
        CreateChannelDialog(
            initialChannel = channel,
            onDismiss = { showCreateDialog = false },
            onCreate = {
                showCreateDialog = false
                joinChannel(it, "我的频道")
            }
        )
    }
}

@Composable
private fun EmptyNearbyChannels() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = WeTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(WeTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_radar_outlined),
                    null,
                    tint = WeTheme.colorScheme.textTertiary,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "正在寻找附近频道",
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "请确认设备处于同一 Wi‑Fi 网络",
                color = WeTheme.colorScheme.textTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LobbyHero() {
    val glow by rememberInfiniteTransition(label = "lobbyGlow").animateFloat(
        initialValue = .86f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "glow"
    )
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF10231A), Color(0xFF173D2A), Color(0xFF0C1712))
                    )
                )
                .padding(24.dp)
        ) {
            Column(Modifier.widthIn(max = 260.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = .1f)
                ) {
                    Text(
                        "LOCAL  ·  PRIVATE",
                        color = Color(0xFF7DE5A9),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    "让附近的声音\n即时抵达",
                    color = Color.White,
                    fontSize = 29.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "加入同一频道，即可收听或按住讲话",
                    color = Color.White.copy(alpha = .62f),
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .scale(glow)
                    .size(82.dp)
                    .background(Color(0xFF07C160).copy(alpha = .16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .background(Color(0xFF07C160), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_mic2_filled),
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = WeTheme.colorScheme.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun CurrentConnectionCard(mode: ConnectionMode) {
    val available = true
    val label = when (mode) {
        ConnectionMode.WiFiLan -> "Wi‑Fi 局域网"
        ConnectionMode.WiFiDirect -> "Wi‑Fi 直连"
        ConnectionMode.Bluetooth -> "蓝牙"
    }
    val detail = when (mode) {
        ConnectionMode.WiFiLan -> "UDP 局域网广播 · 自动发现附近频道"
        ConnectionMode.WiFiDirect -> "通过已连接的 Wi‑Fi Direct 设备传输"
        ConnectionMode.Bluetooth -> "通过已连接的蓝牙设备传输 · 适合少量成员"
    }
    Surface(shape = RoundedCornerShape(18.dp), color = WeTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        WeTheme.colorScheme.primary.copy(alpha = .12f),
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(if (mode == ConnectionMode.Bluetooth) R.drawable.ic_bluetooth_outlined else R.drawable.ic_radar_outlined),
                    null,
                    tint = WeTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(detail, color = WeTheme.colorScheme.textTertiary, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        Color(0xFF43D17A),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun JoinCard(
    channel: String,
    enabled: Boolean,
    onChannelChanged: (String) -> Unit,
    onJoin: () -> Unit
) {
    Surface(shape = RoundedCornerShape(20.dp), color = WeTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "输入频道号",
                color = WeTheme.colorScheme.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = channel,
                onValueChange = onChannelChanged,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = {
                    Text(
                        "# ",
                        color = WeTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                placeholder = { Text("1–999999") },
                shape = RoundedCornerShape(14.dp)
            )
            Button(
                onClick = onJoin,
                enabled = enabled && channel.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WeTheme.colorScheme.primary)
            ) {
                Text("加入频道", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NearbyChannelCard(
    channel: String,
    title: String,
    members: Int,
    activity: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = WeTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(
                        WeTheme.colorScheme.primary.copy(alpha = .12f),
                        RoundedCornerShape(14.dp)
                    ), contentAlignment = Alignment.Center
            ) {
                Text(
                    "#",
                    color = WeTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "#$channel  ·  $members 人在线",
                    color = WeTheme.colorScheme.textTertiary,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    activity,
                    color = if (activity == "安静") WeTheme.colorScheme.textTertiary else WeTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(5.dp))
                Icon(
                    painterResource(R.drawable.ic_right_outlined),
                    null,
                    tint = WeTheme.colorScheme.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateChannelDialog(
    initialChannel: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialChannel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建频道") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "附近设备可通过频道号加入。音频仅在本地网络传输。",
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value,
                    { value = it.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    prefix = { Text("# ") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onCreate(value) }) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

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
            viewModel.setSpeaking(false)
            viewModel.leave()
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
                    Box(Modifier
                        .size(7.dp)
                        .background(Color(0xFF43D17A), CircleShape))
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
                Spacer(Modifier
                    .navigationBarsPadding()
                    .height(20.dp))
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
                onBack()
            }
        )
    }
}

@Composable
private fun SpeakingStage(isTalking: Boolean, speakers: List<IntercomMember>) {
    val remoteSpeakers = speakers.filterNot(IntercomMember::isMe)
    val primarySpeaker = speakers.firstOrNull()
    val isActive = isTalking || speakers.isNotEmpty()
    val pulse by rememberInfiniteTransition(label = "speakerPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(148.dp)
                    .graphicsLayer {
                        scaleX = if (isActive) pulse else 1f; scaleY = if (isActive) pulse else 1f
                    }
                    .background(
                        Color(0xFF07C160).copy(alpha = if (isActive) .18f else .08f),
                        CircleShape
                    )
            )
            Box(
                Modifier
                    .size(116.dp)
                    .background(Color(0xFF183D2A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isTalking) "我" else primarySpeaker?.nickname?.take(1) ?: "—",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(42.dp)
                    .background(if (isActive) Color(0xFF07C160) else Color(0xFF53665B), CircleShape)
                    .border(4.dp, Color(0xFF0D1D15), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_mic_filled),
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            when {
                isTalking -> "你正在讲话"
                remoteSpeakers.isNotEmpty() -> remoteSpeakers.joinToString("、") { it.nickname }
                speakers.any(IntercomMember::isMe) -> "你正在讲话"
                else -> "等待讲话"
            },
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(7.dp))
        Text(
            if (isActive) "声音实时传输中" else "声音已连接 · 可同时发言",
            color = Color.White.copy(alpha = .45f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PushToTalkButton(isTalking: Boolean, onTalkingChanged: (Boolean) -> Unit) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (isTalking) .94f else 1f,
        label = "pttScale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(112.dp)
            .background(if (isTalking) Color(0xFF19D672) else Color(0xFF07C160), CircleShape)
            .border(10.dp, Color.White.copy(alpha = .08f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onTalkingChanged(true)
                        tryAwaitRelease()
                        onTalkingChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_mic2_filled),
            "按住讲话",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun RoomControls(
    speakerEnabled: Boolean,
    onSpeakerToggle: () -> Unit,
    onMembers: () -> Unit,
    connectionLabel: String
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        RoundControl(
            icon = if (speakerEnabled) R.drawable.ic_speaker_filled else R.drawable.ic_speaker_off_filled,
            label = if (speakerEnabled) "扬声器" else "已静音",
            active = speakerEnabled,
            onClick = onSpeakerToggle
        )
        RoundControl(R.drawable.ic_group_chat_outlined, "成员", true, onMembers)
        RoundControl(R.drawable.ic_radar_outlined, connectionLabel, true, {})
    }
}

@Composable
private fun RoundControl(icon: Int, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = if (active) .1f else .04f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(icon),
                null,
                tint = Color.White.copy(alpha = if (active) .9f else .4f),
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = Color.White.copy(alpha = .55f), fontSize = 10.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersSheet(members: List<IntercomMember>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = WeTheme.colorScheme.elevated) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "在线成员",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeTheme.colorScheme.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${members.size} 人",
                    color = WeTheme.colorScheme.textTertiary,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            if (members.isEmpty()) {
                Text(
                    "正在同步频道成员…",
                    color = WeTheme.colorScheme.textTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    textAlign = TextAlign.Center
                )
            }
            members.forEach { member ->
                val displayName = if (member.isMe) "${member.nickname}（你）" else member.nickname
                val status = if (member.isSpeaking) "正在讲话" else "在线"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .background(
                                if (member.isSpeaking) WeTheme.colorScheme.primary.copy(alpha = .16f) else WeTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            member.nickname.take(1),
                            color = if (member.isSpeaking) WeTheme.colorScheme.primary else WeTheme.colorScheme.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        displayName,
                        color = WeTheme.colorScheme.textPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedVisibility(member.isSpeaking, enter = fadeIn(), exit = fadeOut()) {
                        Icon(
                            painterResource(R.drawable.ic_voice_outlined),
                            null,
                            tint = WeTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        status,
                        color = if (member.isSpeaking) WeTheme.colorScheme.primary else WeTheme.colorScheme.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
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
                    "语音格式：16 kHz PCM · 低延迟",
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
