package top.chengdongqing.wechat.feature.chat.ui.session.effect

import android.annotation.SuppressLint
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private data class Debris(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val spin: Float
)

@Composable
fun BombMessageEffect(
    trigger: Int,
    onProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }
    val context = LocalContext.current
    val debris = remember(trigger) {
        val random = Random(trigger * 7919 + 17)
        List(72) {
            Debris(
                angle = random.nextFloat() * (PI * 2).toFloat(),
                speed = 180f + random.nextFloat() * 620f,
                size = 3f + random.nextFloat() * 12f,
                color = listOf(
                    Color(0xFFFFD54F), Color(0xFFFF7A00), Color(0xFFFF3D00),
                    Color.White, Color(0xFF2D2D2D)
                )[random.nextInt(5)],
                spin = random.nextFloat() * (PI * 2).toFloat()
            )
        }
    }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val vibrator = context.getSystemService(Vibrator::class.java)
        @SuppressLint("MissingPermission")
        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 35, 25, 70, 35, 120),
                intArrayOf(0, 90, 0, 180, 0, 110),
                -1
            )
        )
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1_350, easing = LinearEasing)) {
            onProgress(value)
        }
        onProgress(1f)
    }

    if (trigger == 0 || progress.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        val center = Offset(size.width * .5f, size.height * .43f)
        val flashAlpha = ((.24f - p) / .24f).coerceIn(0f, 1f)
        drawCircle(
            Color.White.copy(alpha = flashAlpha * .82f),
            radius = size.maxDimension * (.18f + p),
            center = center
        )

        val shock = (p / .58f).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFFFFB300).copy(alpha = (1f - shock) * .85f),
            radius = 30f + shock * size.minDimension * .58f,
            center = center,
            style = Stroke(width = 18f * (1f - shock) + 2f)
        )
        drawCircle(
            color = Color.White.copy(alpha = (1f - shock) * .75f),
            radius = 18f + shock * size.minDimension * .42f,
            center = center,
            style = Stroke(width = 6f)
        )

        val particleP = (p / .82f).coerceIn(0f, 1f)
        debris.forEachIndexed { index, item ->
            val distance = item.speed * particleP
            val gravity = 430f * particleP * particleP
            val position = Offset(
                center.x + cos(item.angle) * distance,
                center.y + sin(item.angle) * distance + gravity
            )
            val alpha = (1f - particleP).coerceIn(0f, 1f)
            if (index % 3 == 0) {
                val tail = Offset(
                    position.x - cos(item.angle) * item.size * 3,
                    position.y - sin(item.angle) * item.size * 3
                )
                drawLine(item.color.copy(alpha = alpha), tail, position, item.size * .55f)
            } else {
                val path = Path().apply {
                    val rotation = item.spin + particleP * 8f
                    moveTo(
                        position.x + cos(rotation) * item.size,
                        position.y + sin(rotation) * item.size
                    )
                    lineTo(
                        position.x + cos(rotation + 2.3f) * item.size,
                        position.y + sin(rotation + 2.3f) * item.size
                    )
                    lineTo(
                        position.x + cos(rotation + 4.3f) * item.size,
                        position.y + sin(rotation + 4.3f) * item.size
                    )
                    close()
                }
                drawPath(path, item.color.copy(alpha = alpha))
            }
        }

        if (p < .55f) {
            val cloudP = (p / .55f).coerceIn(0f, 1f)
            repeat(9) { index ->
                val angle = index * (PI * 2 / 9).toFloat()
                val radius = 18f + cloudP * 115f
                drawCircle(
                    color = if (index % 2 == 0) Color(0xFFFF6D00) else Color(0xFFFFC400),
                    radius = (72f * (1f - cloudP) + 12f),
                    center = Offset(
                        center.x + cos(angle) * radius,
                        center.y + sin(angle) * radius
                    ),
                    alpha = (1f - cloudP).coerceIn(0f, 1f)
                )
            }
        }
    }
}

data class BombShakeTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

/**
 * 一次冲击而非循环摇摆：短暂错峰后快速炸开，随后以不同频率带阻尼回弹。
 * phase 来自消息 id，因此每条消息的方向稳定，但彼此不会机械同步。
 */
fun bombShakeTransform(progress: Float, phase: Float): BombShakeTransform {
    if (progress >= .86f) return BombShakeTransform()

    val delay = (phase / (PI * 2).toFloat()) * .055f
    val t = ((progress - delay) / (.86f - delay)).coerceIn(0f, 1f)
    if (t <= 0f) return BombShakeTransform()

    // 冲击在前 8% 内迅速建立，之后按物理阻尼衰减。
    val attack = (t / .08f).coerceIn(0f, 1f)
    val damping = exp(-4.15f * t) * attack
    val direction = if (sin(phase) >= 0f) 1f else -1f

    // 主冲击 + 高频余震，避免单一正弦带来的“左右摆动感”。
    val primary = sin(t * PI.toFloat() * 8.6f + phase * .22f)
    val aftershock = sin(t * PI.toFloat() * 19.5f + phase * 1.7f) * .28f
    val vertical = sin(t * PI.toFloat() * 11.7f + phase * .83f)

    // 最初向爆炸外侧猛推，紧接着进入弹性回位。
    val blastPush = direction * (1f - (t / .2f).coerceIn(0f, 1f)) * 52f * attack
    val x = blastPush + (primary + aftershock) * 43f * damping
    val y = (-34f * (1f - (t / .16f).coerceIn(0f, 1f)) * attack) +
        vertical * 20f * damping
    val rotation = direction * 5.2f * damping * cos(t * PI.toFloat() * 7.4f + phase)
    val scale = 1f + 0.055f * damping * sin(t * PI.toFloat() * 5.2f).coerceAtLeast(-.35f)

    return BombShakeTransform(x, y, rotation, scale)
}
