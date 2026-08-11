package top.chengdongqing.wechat.feature.intercom.ui

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.intercom.model.NearbyIntercomChannel

@Composable
fun IntercomLobbyScreen(
    onBack: () -> Unit,
    onJoinChannel: (String) -> Unit,
    viewModel: IntercomViewModel = hiltViewModel()
) {
    var channel by remember { mutableStateOf("5200") }
    val nearbyChannels by viewModel.channels.collectAsStateWithLifecycle()

    fun joinChannel(id: String) {
        viewModel.join(id)
        onJoinChannel(id)
    }

    Scaffold(
        containerColor = WeTheme.colorScheme.background,
        topBar = { WeTopAppBar(title = "语音对讲", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBouncedOverscrollEffect()
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SectionCard {
                JoinChannel(
                    channel = channel,
                    onChannelChanged = {
                        channel = it.filter(Char::isDigit).take(4)
                    },
                    onJoin = {
                        if (channel.isNotBlank()) {
                            joinChannel(channel)
                        }
                    }
                )
            }

            SectionCard("附近频道") {
                if (nearbyChannels.isEmpty()) {
                    EmptyNearbyChannels()
                } else {
                    Column {
                        nearbyChannels.forEachIndexed { index, channel ->
                            NearbyChannelItem(
                                channel = channel,
                                onClick = { joinChannel(channel.id) }
                            )
                            if (index < nearbyChannels.lastIndex) {
                                WeDivider(modifier = Modifier.padding(start = 72.dp))
                            }
                        }
                    }
                }
            }

            Tips()
        }
    }
}

@Composable
private fun Tips() {
    Text(
        text = "语音对讲会在局域网内公共广播，不会加密处理，请注意保护隐私。",
        color = WeTheme.colorScheme.textTertiary,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun JoinChannel(
    channel: String,
    onChannelChanged: (String) -> Unit,
    onJoin: () -> Unit
) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "输入频道号",
            color = WeTheme.colorScheme.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        WeInput(
            label = "#",
            value = channel,
            onValueChange = onChannelChanged,
            placeholder = "1–9999",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            maxLength = 4
        )
        WeButton(
            text = "加入频道",
            width = Dp.Unspecified,
            modifier = Modifier.fillMaxWidth(),
            enabled = channel.isNotBlank(),
            onClick = onJoin
        )
    }
}

@Composable
private fun NearbyChannelItem(
    channel: NearbyIntercomChannel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    color = WeTheme.colorScheme.primary.copy(alpha = .12f),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_radar_outlined),
                contentDescription = null,
                tint = WeTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "# ${channel.id}",
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${channel.memberCount} 人在线",
                color = WeTheme.colorScheme.textTertiary,
                fontSize = 12.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (channel.speakingCount == 0) {
                    "安静"
                } else {
                    "${channel.speakingCount} 人正在讲话"
                },
                color = if (channel.speakingCount == 0) {
                    WeTheme.colorScheme.textTertiary
                } else {
                    WeTheme.colorScheme.primary
                },
                fontSize = 11.sp
            )
            Spacer(Modifier.height(5.dp))
            Icon(
                painter = painterResource(R.drawable.ic_right_outlined),
                contentDescription = null,
                tint = WeTheme.colorScheme.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyNearbyChannels() {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarRotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    Column(
        modifier = Modifier.padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(WeTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_radar_outlined),
                contentDescription = null,
                tint = WeTheme.colorScheme.textTertiary,
                modifier = Modifier
                    .size(23.dp)
                    .graphicsLayer {
                        rotationZ = angle
                    }
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "正在扫描附近频道",
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "请确认设备处于同一 Wi‑Fi 下",
            color = WeTheme.colorScheme.textTertiary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SectionCard(
    label: String? = null,
    content: @Composable () -> Unit
) {
    Column {
        label?.let {
            Text(
                text = it,
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = WeTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
