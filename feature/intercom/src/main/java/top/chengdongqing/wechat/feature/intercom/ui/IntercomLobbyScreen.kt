package top.chengdongqing.wechat.feature.intercom.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

