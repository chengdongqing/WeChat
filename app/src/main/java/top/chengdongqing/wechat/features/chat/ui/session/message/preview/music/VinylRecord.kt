package top.chengdongqing.wechat.features.chat.ui.session.message.preview.music

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.White

@Composable
fun VinylRecord(@DrawableRes albumArtRes: Int, isPlaying: Boolean) {
    // 保存暂停时的角度，下次播放从此处继续，避免复位
    var currentRotation by remember { mutableFloatStateOf(0f) }
    val rotationAnimatable = remember { Animatable(0f) }

    // 转速倍率：0f = 停止，1f = 全速（20秒/圈）
    // 对它插值实现启动加速 / 停止减速的惯性效果
    val speedMultiplier = remember { Animatable(0f) }

    // 监听播放状态，驱动转速加减速
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotationAnimatable.snapTo(currentRotation)
            // 800ms 内从 0 加速到全速
            speedMultiplier.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        } else {
            // 1000ms 内从全速减速到 0
            speedMultiplier.animateTo(
                targetValue = 0f,
                animationSpec = tween(1000, easing = FastOutSlowInEasing)
            )
            // 完全停稳后记录角度，供下次播放接续
            currentRotation = rotationAnimatable.value % 360f
        }
    }

    // 每帧根据真实帧间隔 × 当前转速倍率累加角度
    // 用 withFrameMillis 而非固定 tween，保证变速时角度连续平滑
    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (true) {
            withFrameMillis { frameTime ->
                if (lastFrameTime != 0L) {
                    val deltaMs = frameTime - lastFrameTime
                    // 全速：20 秒一圈 → 每毫秒转 360/20000 度
                    val degreesPerMs = 360f / 20_000f
                    val delta = degreesPerMs * deltaMs * speedMultiplier.value
                    currentRotation = (currentRotation + delta) % 360f
                    launch { rotationAnimatable.snapTo(currentRotation) }
                }
                lastFrameTime = frameTime
            }
        }
    }

    // 唱头臂摆角
    val stylusRotation by animateFloatAsState(
        targetValue = if (isPlaying) 0f else -26f,
        animationSpec = tween(500),
        label = "stylusRotation"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(1f),
        contentAlignment = Alignment.TopCenter
    ) {
        val vinylSize = maxWidth
        val baseVinylSize = 300.dp
        val scale = vinylSize / baseVinylSize

        val stylusSize = 300.dp * scale
        val stylusOffsetY = (-220).dp * scale

        // 唱片主体
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .clip(CircleShape)
                .background(White.copy(alpha = 0.1f))
                .padding(8.dp)
                .rotate(rotationAnimatable.value)
                .shadow(30.dp, CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2

                // 黑胶底色
                drawCircle(Color(0xFF111111))

                // 环形音轨纹理
                for (i in 1..30) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = radius * (0.4f + i * 0.02f),
                        style = Stroke(width = 1f)
                    )
                }

                // 扫描式高光
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.12f to Color.White.copy(alpha = 0.12f),
                        0.25f to Color.Transparent,
                        0.62f to Color.Transparent,
                        0.75f to Color.White.copy(alpha = 0.10f),
                        0.88f to Color.Transparent,
                        1.00f to Color.Transparent,
                        center = center
                    ),
                    radius = radius * 0.98f,
                    blendMode = BlendMode.Screen
                )
            }

            // 专辑封面
            Image(
                painter = painterResource(albumArtRes),
                contentDescription = "专辑封面",
                modifier = Modifier
                    .fillMaxSize(0.70f)
                    .aspectRatio(1f)
                    .align(Alignment.Center)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // 唱头臂
        Box(modifier = Modifier.offset(y = stylusOffsetY)) {
            // 轴销底座装饰圆
            Spacer(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.1f))
            )
            Image(
                painter = painterResource(id = R.drawable.img_stylus),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .size(stylusSize)
                    .rotate(stylusRotation)
            )
        }
    }
}