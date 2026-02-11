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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RadarScanScreen(onBack: () -> Unit) {
    val fakeUsers = remember {
        listOf(
            RadarUser("1", R.drawable.img_avatar, 45.0, 0.6f),
            RadarUser("2", R.drawable.img_avatar, 150.0, 0.8f),
            RadarUser("3", R.drawable.img_avatar, 280.0, 1f)
        )
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

        MyAvatar()
        DiscoveredUsers(users = fakeUsers)

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
        Text(text = "退出", color = Color.Gray)
    }
}

@Composable
private fun BoxScope.MyAvatar() {
    Box(
        modifier = Modifier
            .size(74.dp)
            .clip(CircleShape)
            .background(Color.White)
            .padding(2.dp)
            .align(Alignment.Center)
    ) {
        Image(
            painter = painterResource(R.drawable.img_avatar),
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
private fun DiscoveredUsers(users: List<RadarUser>) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 计算雷达可用半径（取宽高最小值的一半，再留点边距）
        val maxRadius = min(constraints.maxWidth, constraints.maxHeight) / 2f * 0.8f

        users.forEach { user ->
            val offset = calculateOffset(user.angle, user.distance, maxRadius)

            Box(
                modifier = Modifier
                    .align(Alignment.Center) // 先居中
                    .offset { offset }       // 根据极坐标偏移
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            ) {
                Image(
                    painter = painterResource(user.avatarRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun calculateOffset(angle: Double, distance: Float, maxRadius: Float): IntOffset {
    val radian = Math.toRadians(angle)
    val x = (distance * maxRadius * cos(radian)).toInt()
    val y = (distance * maxRadius * sin(radian)).toInt()
    return IntOffset(x, y)
}

private data class RadarUser(
    val id: String,
    val avatarRes: Int,
    val angle: Double, // 0..360
    val distance: Float // 0..1 (0代表中心，1代表雷达边缘)
)