package top.chengdongqing.wechat.features.contacts.ui.add.nfc.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.theme.GreenPrimary
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.NfcAddState

@Composable
fun NfcConnected(
    contact: Contact,
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
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "card_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .padding(horizontal = 36.dp)
            .fillMaxWidth()
    ) {
        ConnectedBadge()
        Spacer(Modifier.height(24.dp))
        PeerAvatar(contact = contact, addState = addState)
        Spacer(Modifier.height(22.dp))
        Text(
            text = contact.nickname,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = WeTheme.colorScheme.textPrimary
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "微信号: ${contact.id}",
            fontSize = 12.sp,
            color = WeTheme.colorScheme.textSecondary
        )
        contact.signature?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                fontSize = 13.sp,
                color = WeTheme.colorScheme.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 20.sp
            )
        }
        Spacer(Modifier.height(44.dp))
        AddActionArea(addState = addState, onAddFriend = onAddFriend)
    }
}

// "碰一碰成功" 角标
@Composable
private fun ConnectedBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GreenPrimary.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(GreenPrimary)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "碰一碰成功",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = GreenPrimary
        )
    }
}

/**
 * 对方头像。
 * - 优先展示本地字节 > URL > 昵称首字兜底
 * - 添加成功时触发扩散光环动画
 */
@Composable
private fun PeerAvatar(contact: Contact, addState: NfcAddState) {
    val isSuccess = addState is NfcAddState.Success

    Box(contentAlignment = Alignment.Center) {
        if (isSuccess) {
            ExpandingRing()
        }

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A2F45))
                .border(
                    width = if (isSuccess) 3.dp else 2.dp,
                    color = if (isSuccess) GreenPrimary else GreenPrimary.copy(alpha = 0.45f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                contact.avatarPath != null ->
                    AsyncImage(
                        model = contact.avatarPath,
                        contentDescription = contact.nickname,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )

                else ->
                    Text(
                        text = contact.nickname.take(1),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = WeTheme.colorScheme.textPrimary
                    )
            }
        }
    }
}

// 添加成功时头像外圈的扩散光环
@Composable
private fun ExpandingRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "success_ring")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )
    Box(
        modifier = Modifier
            .size(108.dp)
            .scale(ringScale)
            .alpha(ringAlpha)
            .clip(CircleShape)
            .border(2.dp, GreenPrimary, CircleShape)
    )
}

/**
 * 根据 [addState] 渲染不同操作内容，所有状态切换带 fade + scale 动画。
 */
@Composable
private fun AddActionArea(addState: NfcAddState, onAddFriend: () -> Unit) {
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
                NfcActionButton(
                    text = "添加到通讯录",
                    enabled = true,
                    onClick = onAddFriend
                )

            is NfcAddState.PeerReady ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PeerReadyHint()
                    Spacer(Modifier.height(18.dp))
                    NfcActionButton(
                        text = "添加到通讯录",
                        enabled = true,
                        onClick = onAddFriend
                    )
                }

            is NfcAddState.WaitingForPeer ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NfcActionButton(text = "等待对方添加...", enabled = false)
                    Spacer(Modifier.height(14.dp))
                    BouncingDots()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "请告知对方点击添加",
                        fontSize = 12.sp,
                        color = WeTheme.colorScheme.textSecondary
                    )
                }

            is NfcAddState.Exchanging ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WeLoading(
                        size = 36.dp,
                        color = GreenPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "正在添加...",
                        fontSize = 15.sp,
                        color = WeTheme.colorScheme.textSecondary
                    )
                }

            is NfcAddState.Success ->
                AddSuccessView()

            is NfcAddState.Timeout ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "等待超时",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = WeTheme.colorScheme.danger
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "对方未在 60 秒内点击添加",
                        fontSize = 13.sp,
                        color = WeTheme.colorScheme.textSecondary
                    )
                    Spacer(Modifier.height(24.dp))
                    NfcActionButton(text = "重新发起", enabled = true, onClick = onAddFriend)
                }

            is NfcAddState.Error ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        fontSize = 14.sp,
                        color = WeTheme.colorScheme.danger,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    NfcActionButton(text = "重试", enabled = true, onClick = onAddFriend)
                }
        }
    }
}

// 对方已点击添加时的绿色提示条（带呼吸圆点）
@Composable
private fun PeerReadyHint() {
    val infiniteTransition = rememberInfiniteTransition(label = "peer_ready")
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
            .background(GreenPrimary.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(GreenPrimary.copy(alpha = dotAlpha))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "对方已点击添加，等你确认",
            fontSize = 13.sp,
            color = GreenPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

// 等待对方时的三点跳动动画
@Composable
private fun BouncingDots() {
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
                    .background(GreenPrimary.copy(alpha = alpha))
            )
        }
    }
}

// 添加成功的勾选弹出动画
@Composable
private fun AddSuccessView() {
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
                .background(Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF059647)))),
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
            color = WeTheme.colorScheme.textPrimary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "已加入通讯录，现在可以开始聊天了",
            fontSize = 13.sp,
            color = WeTheme.colorScheme.textSecondary
        )
    }
}