package top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.MusicPreviewScreen

@Preview
@Composable
private fun Preview() {
    WeTheme {
        MusicPreviewScreen { }
    }
}

@Composable
fun VinylRecord(isPlaying: Boolean, albumArt: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ), label = ""
    )

    val stylusRotation by animateFloatAsState(
        targetValue = if (isPlaying) 0f else -30f,
        animationSpec = tween(500), label = ""
    )

    Box(modifier = Modifier.size(340.dp), contentAlignment = Alignment.TopCenter) {
        // 唱片主体
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .rotate(if (isPlaying) rotation else 0f)
                .shadow(30.dp, CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2

                // 黑胶外圈
                drawCircle(Color(0xFF111111))
                // 音轨纹理
                for (i in 1..30) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.minDimension / 2 * (0.4f + i * 0.02f),
                        style = Stroke(width = 1f)
                    )
                }

                // 定义扫描线渐变高光
                // 定义一个从透明 -> 白 -> 透明 -> 白 -> 透明的周期渐变
                val sweepGradientBrush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.12f to Color.White.copy(alpha = 0.12f), // 第一条光斑中心
                    0.25f to Color.Transparent,
                    0.62f to Color.Transparent, // 对角区域
                    0.75f to Color.White.copy(alpha = 0.1f), // 第二条光斑中心（稍微暗点）
                    0.88f to Color.Transparent,
                    1.0f to Color.Transparent,
                    center = center
                )

                // 绘制高光层
                // 将高光层画在一个稍小的圆内，避免覆盖最外圈边缘
                drawCircle(
                    brush = sweepGradientBrush,
                    radius = radius * 0.98f, // 留一点边缘
                    blendMode = BlendMode.Screen // 混合模式是关键
                )
            }
            // 专辑封面
            Image(
                painter = painterResource(albumArt),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // 唱头
        Box(modifier = Modifier.offset(y = (-185).dp)) {
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
                modifier = Modifier
                    .size(280.dp)
                    .rotate(stylusRotation)
            )
        }
    }
}