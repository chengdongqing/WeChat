package top.chengdongqing.wechat.features.contacts.ui.add.radar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.rememberToastState
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScanScreen(
    onBack: () -> Unit,
    onNavigateToContact: (id: String) -> Unit,
    viewModel: RadarScanViewModel = hiltViewModel()
) {
    val radarUsers by viewModel.radarUsers.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val loadingUserId by viewModel.loadingUserId.collectAsStateWithLifecycle()
    val navigateToContact by viewModel.navigateToContact.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // 跳转联系人详情
    LaunchedEffect(navigateToContact) {
        navigateToContact?.let {
            onNavigateToContact(it.id)
            viewModel.onNavigateConsumed()
        }
    }

    val toast = rememberToastState()
    // 错误提示
    LaunchedEffect(error) {
        error?.let {
            toast.show(title = it, icon = ToastIcon.Fail)
            viewModel.onErrorConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Image(
            painter = painterResource(R.drawable.img_radar_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        RotatingRadar()
        MyAvatar(myProfile)
        DiscoveredUsers(
            users = radarUsers,
            loadingUserId = loadingUserId,
            onUserClick = viewModel::onUserClicked
        )
        BackButton(onBack)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .offset(20.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
            .clickable { onBack() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.action_exit),
            color = Color.Gray
        )
    }
}

@Composable
private fun BoxScope.MyAvatar(myProfile: UserProfile?) {
    Box(
        modifier = Modifier
            .size(74.dp)
            .clip(CircleShape)
            .background(Color.White)
            .padding(2.dp)
            .align(Alignment.Center)
    ) {
        AsyncImage(
            model = myProfile?.avatarPath,
            error = painterResource(R.drawable.img_avatar_placeholder),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
private fun RotatingRadar() {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarRotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    Image(
        painter = painterResource(id = R.drawable.img_radar),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = angle
                scaleX = 1.5f
                scaleY = 1.5f
            }
    )
}

@Composable
private fun DiscoveredUsers(
    users: List<RadarUser>,
    loadingUserId: String?,
    onUserClick: (RadarUser) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val avatarSizeDp = remember(users.size) {
            when {
                users.size >= 20 -> 60.dp
                users.size >= 10 -> 80.dp
                else -> 100.dp
            }
        }
        val avatarSizePx = with(density) { avatarSizeDp.toPx() }

        // 分别计算横向和纵向的安全半径，减去头像尺寸和必要的边距
        val margin = with(density) { 20.dp.toPx() }
        val safeWidthRadius = (constraints.maxWidth / 2f) - (avatarSizePx / 2f) - margin
        val safeHeightRadius = (constraints.maxHeight / 2f) - (avatarSizePx / 2f) - margin

        users.forEach { user ->
            // 传入两个半径进行椭圆计算
            val offset = remember(user.angle, user.distance, safeWidthRadius, safeHeightRadius) {
                calculateOffset(user.angle, user.distance, safeWidthRadius, safeHeightRadius)
            }

            key(user.id) {
                UserAvatar(
                    user = user,
                    size = avatarSizeDp,
                    isLoading = user.id == loadingUserId,
                    onClick = { onUserClick(user) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset {
                            IntOffset(offset.x, offset.y)
                        }
                )
            }
        }
    }
}

@Composable
private fun UserAvatar(
    user: RadarUser,
    size: Dp,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = size),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size((size.value * 0.6).dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                .weClickable(enabled = !isLoading, onClick = onClick)
        ) {
            AsyncImage(
                model = user.avatarUrl,
                error = painterResource(R.drawable.img_avatar_placeholder),
                contentDescription = user.nickname,
                modifier = Modifier.fillMaxSize()
            )

            // loading 遮罩
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    WeLoading(size = 24.dp, color = White)
                }
            }
        }

        Text(
            text = user.nickname,
            color = White,
            fontSize = 13.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun calculateOffset(
    angle: Double,
    distance: Float,
    safeWidthRadius: Float,
    safeHeightRadius: Float
): IntOffset {
    val radian = Math.toRadians(angle)
    val x = (distance * safeWidthRadius * cos(radian)).toInt()
    val y = (distance * safeHeightRadius * sin(radian)).toInt()
    return IntOffset(x, y)
}