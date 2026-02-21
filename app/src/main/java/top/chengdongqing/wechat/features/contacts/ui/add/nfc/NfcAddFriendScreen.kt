package top.chengdongqing.wechat.features.contacts.ui.add.nfc

import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.util.StatusBarAppearanceEffect
import top.chengdongqing.wechat.core.nfc.NfcHceService

// ==================== 背景色常量 ====================

private val BgTop = Color(0xFF0A1628)
private val BgBottom = Color(0xFF0F2240)
private val Green = Color(0xFF07C160)

// ==================== 主屏幕入口 ====================

@Composable
fun NfcAddFriendScreen(
    viewModel: NfcAddFriendViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val nfcAvailability = rememberNfcAvailability()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isReaderMode by remember { mutableStateOf(true) }

    StatusBarAppearanceEffect(false)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgTop, BgBottom),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        when (nfcAvailability) {
            NfcAvailability.NotSupported ->
                NfcUnavailableScreen(
                    title = "设备不支持 NFC",
                    description = "你的手机没有 NFC 芯片，无法使用碰一碰功能。\n可以使用扫一扫来添加好友。",
                    actionLabel = null,
                    onBack = onBack
                )

            NfcAvailability.Disabled ->
                NfcUnavailableScreen(
                    title = "NFC 未开启",
                    description = "请前往系统设置开启 NFC 功能后，再回来使用碰一碰。",
                    actionLabel = "前往开启 NFC",
                    onAction = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                    onBack = onBack
                )

            NfcAvailability.Enabled -> {
                HcePreferredService()

                NfcReaderDispatch(isReaderMode = isReaderMode) { userId ->
                    viewModel.onNfcDetected(userId)
                }

                NfcAddFriendContent(
                    uiState = uiState,
                    isReaderMode = isReaderMode,
                    onModeChange = { isReaderMode = it },
                    onBack = onBack,
                    onAddFriend = { viewModel.onAddFriend() }
                )
            }
        }
    }
}

@Composable
fun HcePreferredService() {
    val context = LocalContext.current
    val activity = LocalActivity.current ?: return

    DisposableEffect(Unit) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        val cardEmulation = nfcAdapter?.let { CardEmulation.getInstance(it) }
        val componentName = ComponentName(context, NfcHceService::class.java)

        if (cardEmulation != null) {
            cardEmulation.setPreferredService(activity, componentName)
            Log.d("NfcHce", "✅ 已设置 HCE 前台优先级")
        }

        onDispose {
            cardEmulation?.unsetPreferredService(activity)
            Log.d("NfcHce", "✅ 已释放 HCE 前台优先级")
        }
    }
}

// ==================== 内容层（背景透明，复用外层背景） ====================

@Composable
fun NfcAddFriendContent(
    uiState: NfcAddFriendUiState,
    isReaderMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAddFriend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── 自定义顶栏（透明背景，延伸到状态栏） ──
        NfcTopBar(onBack = onBack)

        NfcModeSwitcher(isReaderMode, onModeChange)

        // ── 内容区域居中 ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.94f))
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "nfc_content"
            ) { state ->
                when (state.connectionState) {
                    is NfcConnectionState.Waiting ->
                        WaitingContent()

                    is NfcConnectionState.Connecting ->
                        ConnectingContent()

                    is NfcConnectionState.Connected ->
                        ConnectedContent(
                            peerProfile = state.peerProfile!!,
                            addState = state.addState,
                            onAddFriend = onAddFriend
                        )

                    is NfcConnectionState.Failed ->
                        FailedContent(
                            reason = state.connectionState.reason,
                            onRetry = { /* 触发重试逻辑 */ }
                        )
                }
            }
        }

        // ── 底部安全区留白 ──
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ColumnScope.NfcModeSwitcher(isReaderMode: Boolean, onModeChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 20.dp)
            .align(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(4.dp)
    ) {
        listOf(true to "去扫描", false to "被扫描").forEach { (mode, label) ->
            val selected = isReaderMode == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Green else Color.Transparent)
                    .clickable { onModeChange(mode) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else Color.White.copy(0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==================== 自定义顶栏（透明，适配状态栏） ====================

@Composable
private fun NfcTopBar(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)   // 正确空出状态栏高度
            .height(56.dp)
            .padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "碰一碰",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// ==================== 等待碰一碰 ====================

@Composable
private fun WaitingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    ) {
        PulsingNfcIcon()

        Spacer(Modifier.height(52.dp))

        Text(
            text = "将手机靠近对方手机",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "背靠背或正面相对均可\n碰触后自动拉取对方信息",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun PulsingNfcIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")

    data class RingConfig(val delayMs: Int, val sizeDp: Float, val maxAlpha: Float)

    val rings = listOf(
        RingConfig(0, 140f, 0.50f),
        RingConfig(450, 190f, 0.28f),
        RingConfig(900, 245f, 0.12f)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(270.dp)
    ) {
        rings.forEach { ring ->
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2000,
                        delayMillis = ring.delayMs,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ring_${ring.delayMs}"
            )
            Box(
                modifier = Modifier
                    .size(ring.sizeDp.dp)
                    .scale(0.5f + progress * 0.5f)
                    .alpha((1f - progress) * ring.maxAlpha)
                    .clip(CircleShape)
                    .border(width = 1.5.dp, color = Green, shape = CircleShape)
            )
        }

        // 中心圆
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Green.copy(alpha = 0.20f),
                            Green.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(2.dp, Green.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_radar_outlined),
                contentDescription = "NFC",
                tint = Green,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

// ==================== 连接中 ====================

@Composable
private fun ConnectingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "connecting")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 40.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Green.copy(alpha = 0.08f))
                .border(2.dp, Green.copy(alpha = alpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Green.copy(alpha = alpha),
                strokeWidth = 3.dp,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text = "正在获取对方信息...",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "请保持手机靠近",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

// ==================== 连接成功 ====================

@Composable
private fun ConnectedContent(
    peerProfile: NfcPeerProfile,
    addState: NfcAddState,
    onAddFriend: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "card_scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "card_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .alpha(cardAlpha)
            .padding(horizontal = 36.dp)
            .fillMaxWidth()
    ) {
        // 碰触成功标识
        SuccessBadge()

        Spacer(Modifier.height(24.dp))

        // 头像（成功时有光环）
        PeerAvatarSection(peerProfile, addState)

        Spacer(Modifier.height(22.dp))

        Text(
            text = peerProfile.nickname,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = "微信号: ${peerProfile.id}",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.32f)
        )

        if (peerProfile.signature.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "\"${peerProfile.signature}\"",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(44.dp))

        AddActionSection(addState = addState, onAddFriend = onAddFriend)
    }
}

@Composable
private fun SuccessBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Green.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Green)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "碰一碰成功",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Green
        )
    }
}

@Composable
private fun PeerAvatarSection(peerProfile: NfcPeerProfile, addState: NfcAddState) {
    val isSuccess = addState is NfcAddState.Success

    Box(contentAlignment = Alignment.Center) {
        // 成功时扩散光环
        if (isSuccess) {
            val infiniteTransition = rememberInfiniteTransition(label = "success_ring")
            val ringAlpha by infiniteTransition.animateFloat(
                initialValue = 0.7f, targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ra"
            )
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rs"
            )
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .scale(ringScale)
                    .alpha(ringAlpha)
                    .clip(CircleShape)
                    .border(2.dp, Green, CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A2F45))
                .border(
                    width = if (isSuccess) 3.dp else 2.dp,
                    color = if (isSuccess) Green else Green.copy(alpha = 0.45f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                peerProfile.avatarBytes != null ->
                    AsyncImage(
                        model = peerProfile.avatarBytes,
                        contentDescription = peerProfile.nickname,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )

                peerProfile.avatarUrl.isNotBlank() ->
                    AsyncImage(
                        model = peerProfile.avatarUrl,
                        contentDescription = peerProfile.nickname,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )

                else ->
                    Text(
                        text = peerProfile.nickname.take(1),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
            }
        }
    }
}

// ==================== 添加操作区域 ====================

@Composable
private fun AddActionSection(addState: NfcAddState, onAddFriend: () -> Unit) {
    AnimatedContent(
        targetState = addState,
        transitionSpec = {
            (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.92f))
                .togetherWith(fadeOut(tween(150)))
        },
        label = "add_state"
    ) { state ->
        when (state) {
            is NfcAddState.Idle ->
                AddButton(text = "添加到通讯录", enabled = true, onClick = onAddFriend)

            is NfcAddState.PeerReady ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PeerReadyHint()
                    Spacer(Modifier.height(18.dp))
                    AddButton(text = "添加到通讯录", enabled = true, onClick = onAddFriend)
                }

            is NfcAddState.WaitingForPeer ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AddButton(text = "等待对方添加...", enabled = false)
                    Spacer(Modifier.height(14.dp))
                    WaitingDots()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "请告知对方点击添加",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.35f)
                    )
                }

            is NfcAddState.Exchanging ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Green,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "正在添加...",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

            is NfcAddState.Success ->
                SuccessSection()

            is NfcAddState.Timeout ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "等待超时",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEF4444)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "对方未在60秒内点击添加",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(24.dp))
                    AddButton(text = "重新发起", enabled = true, onClick = onAddFriend)
                }

            is NfcAddState.Error ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        fontSize = 14.sp,
                        color = Color(0xFFEF4444),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    AddButton(text = "重试", enabled = true, onClick = onAddFriend)
                }
        }
    }
}

@Composable
private fun PeerReadyHint() {
    val infiniteTransition = rememberInfiniteTransition(label = "peer_ready_hint")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Green.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Green.copy(alpha = dotAlpha))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "对方已点击添加，等你确认",
            fontSize = 13.sp,
            color = Green,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WaitingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(0, 180, 360).forEach { delay ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$delay"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Green.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun SuccessSection() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "success_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF059647)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "成功",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "添加成功",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "已加入通讯录，现在可以开始聊天了",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}

// ==================== 失败 ====================

@Composable
private fun FailedContent(reason: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 40.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = 0.10f))
                .border(2.dp, Color(0xFFEF4444).copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close_outlined),
                contentDescription = "失败",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = reason,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "请重新靠近对方手机再试",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.4f)
        )

        Spacer(Modifier.height(36.dp))

        AddButton(text = "重新碰一碰", enabled = true, onClick = onRetry)
    }
}

// ==================== NFC 不可用 ====================

@Composable
private fun NfcUnavailableScreen(
    title: String,
    description: String,
    actionLabel: String?,
    onAction: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NfcTopBar(onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 44.dp)
            ) {
                // 图标圆
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (actionLabel != null) "NFC" else "⚠",
                        fontSize = if (actionLabel != null) 22.sp else 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.height(44.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(Green)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAction
                            )
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = actionLabel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "开启后返回此页面即可使用",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.32f)
                    )
                }
            }
        }
    }
}

// ==================== 通用按钮 ====================

@Composable
private fun AddButton(
    text: String,
    enabled: Boolean,
    backgroundColor: Color = Green,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) backgroundColor else Color.White.copy(alpha = 0.10f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 52.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.35f)
        )
    }
}