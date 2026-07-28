package top.chengdongqing.wechat.feature.chat.ui.session.effect

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
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class FestiveEffectType { Fireworks, Celebration, Firecrackers }

data class FestiveEffectEvent(val serial: Int, val type: FestiveEffectType)

private val festiveColors = listOf(
    Color(0xFFFFD740), Color(0xFFFF4081), Color(0xFF7C4DFF),
    Color(0xFF00E5FF), Color(0xFF69F0AE), Color(0xFFFF6D00)
)

@Composable
fun FestiveMessageEffect(
    event: FestiveEffectEvent?,
    modifier: Modifier = Modifier
) {
    if (event == null) return
    val progress = remember { Animatable(1f) }
    val randomSeed = event.serial * 1543 + event.type.ordinal * 97

    LaunchedEffect(event) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            tween(
                durationMillis = when (event.type) {
                    FestiveEffectType.Fireworks -> 2_700
                    FestiveEffectType.Celebration -> 2_350
                    FestiveEffectType.Firecrackers -> 2_000
                },
                easing = LinearEasing
            )
        )
    }
    if (progress.value >= 1f) return

    Canvas(modifier.fillMaxSize()) {
        when (event.type) {
            FestiveEffectType.Fireworks -> drawFireworks(progress.value, randomSeed)
            FestiveEffectType.Celebration -> drawCelebration(progress.value, randomSeed)
            FestiveEffectType.Firecrackers -> drawFirecrackers(progress.value, randomSeed)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFireworks(
    progress: Float,
    seed: Int
) {
    val random = Random(seed)
    // 压暗聊天背景，让烟花的亮部真正“炸”出来。
    val curtain = sin(progress.coerceIn(0f, 1f) * PI.toFloat()).coerceAtLeast(0f)
    drawRect(Color(0xFF07101E).copy(alpha = curtain * .28f))
    repeat(10) { burst ->
        val start = burst * .068f
        val cycle = ((progress - start) / .36f).coerceIn(0f, 1f)
        if (cycle <= 0f || cycle >= 1f) return@repeat
        val center = Offset(
            size.width * (.08f + random.nextFloat() * .84f),
            size.height * (.09f + random.nextFloat() * .48f)
        )
        val color = festiveColors[random.nextInt(festiveColors.size)]
        // 每一朵先从底部升空，留下明亮彗尾。
        if (cycle < .24f) {
            val launch = cycle / .24f
            val rocket = Offset(
                center.x + sin(launch * PI.toFloat() * 2f + burst) * 8f,
                size.height + (center.y - size.height) * launch
            )
            drawLine(
                color.copy(alpha = .9f),
                Offset(rocket.x, rocket.y + 90f),
                rocket,
                5f
            )
            drawCircle(Color.White, 7f, rocket)
        }
        val local = ((cycle - .2f) / .8f).coerceIn(0f, 1f)
        if (local <= 0f) return@repeat
        val rays = 34 + random.nextInt(20)
        val alpha = (1f - local).let { it * it }
        repeat(rays) { ray ->
            val angle = ray * (PI * 2 / rays).toFloat() + random.nextFloat() * .08f
            val distance = size.minDimension * (.055f + .38f * local) *
                (.75f + random.nextFloat() * .45f)
            val end = Offset(
                center.x + cos(angle) * distance,
                center.y + sin(angle) * distance + 140f * local * local
            )
            val tailLength = 24f + 80f * local
            val tail = Offset(
                end.x - cos(angle) * tailLength,
                end.y - sin(angle) * tailLength
            )
            drawLine(color.copy(alpha = alpha * .24f), tail, end, 10f)
            drawLine(color.copy(alpha = alpha), tail, end, 3.4f + (1f - local) * 3.8f)
            drawCircle(Color.White.copy(alpha = alpha), 3.2f, end)
            if (ray % 3 == 0) {
                val innerDistance = distance * .63f
                drawCircle(
                    color.copy(alpha = alpha * .8f),
                    3.5f,
                    Offset(
                        center.x + cos(angle) * innerDistance,
                        center.y + sin(angle) * innerDistance
                    )
                )
            }
        }
        drawCircle(
            color.copy(alpha = ((.22f - local) / .22f).coerceIn(0f, 1f) * .45f),
            80f + local * 90f,
            center
        )
        drawCircle(
            Color.White.copy(alpha = ((.13f - local) / .13f).coerceIn(0f, 1f)),
            25f + local * 45f,
            center
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCelebration(
    progress: Float,
    seed: Int
) {
    val random = Random(seed)
    // 所有彩带从屏幕底边的两只礼花筒高速冲出。
    repeat(150) { index ->
        val delay = random.nextFloat() * .18f
        val local = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
        if (local <= 0f || local >= 1f) return@repeat
        val fromLeft = index % 2 == 0
        val originX = size.width * if (fromLeft) .18f else .82f
        val direction = if (fromLeft) 1f else -1f
        val velocityX = direction * size.width * (.12f + random.nextFloat() * .75f)
        val velocityY = size.height * (2.05f + random.nextFloat() * .48f)
        val gravity = size.height * (1.5f + random.nextFloat() * .32f)
        val x = originX + velocityX * local +
            sin(local * PI.toFloat() * (2f + random.nextFloat() * 3f)) * 22f
        val y = size.height + 15f - velocityY * local + gravity * local * local
        val color = festiveColors[random.nextInt(festiveColors.size)]
        val particleSize = 6f + random.nextFloat() * 12f
        rotate(random.nextFloat() * 360f + local * 1_200f, Offset(x, y)) {
            drawRect(
                color.copy(alpha = (1f - local * .35f).coerceIn(0f, 1f)),
                topLeft = Offset(x - particleSize, y - particleSize * .35f),
                size = androidx.compose.ui.geometry.Size(particleSize * 2f, particleSize * .7f)
            )
        }
        if (index < 34 && local < .42f) {
            drawLine(
                color.copy(alpha = (1f - local / .42f).coerceIn(0f, 1f)),
                Offset(originX, size.height),
                Offset(x, y),
                2.5f
            )
        }
    }
    val flash = ((.2f - progress) / .2f).coerceIn(0f, 1f)
    drawCircle(
        Color(0xFFFFD740).copy(alpha = flash),
        125f,
        Offset(size.width * .18f, size.height)
    )
    drawCircle(
        Color(0xFFFFD740).copy(alpha = flash),
        125f,
        Offset(size.width * .82f, size.height)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFirecrackers(
    progress: Float,
    seed: Int
) {
    val random = Random(seed)
    val chainTop = size.height * .12f
    val chainBottom = size.height * .72f
    val swing = sin(progress * PI.toFloat() * 7f) * 24f * (1f - progress * .65f)
    val chainX = size.width * .5f + swing
    drawLine(
        Color(0xFFD8A21B).copy(alpha = (1f - progress).coerceAtLeast(.25f)),
        Offset(size.width * .5f, chainTop),
        Offset(chainX, chainBottom),
        5f
    )
    repeat(13) { index ->
        val y = chainTop + (chainBottom - chainTop) * index / 12f
        val side = if (index % 2 == 0) -1f else 1f
        val explodedAt = index * .046f
        val local = ((progress - explodedAt) / .3f).coerceIn(0f, 1f)
        val swayByHeight = swing * (index / 13f)
        val kick = if (local in .01f..1f) {
            sin(local * PI.toFloat() * 5f) * (1f - local) * 22f
        } else 0f
        val crackerCenter = Offset(chainX + side * 27f + swayByHeight + kick * side, y)
        if (local < .2f) {
            rotate(side * (24f + swing * .35f) + kick, crackerCenter) {
                drawRoundRect(
                    Color(0xFFE3262E),
                    topLeft = Offset(crackerCenter.x - 11f, crackerCenter.y - 25f),
                    size = androidx.compose.ui.geometry.Size(22f, 50f)
                )
                drawLine(
                    Color(0xFFFFD54F),
                    Offset(crackerCenter.x - 9f, crackerCenter.y),
                    Offset(crackerCenter.x + 9f, crackerCenter.y),
                    3f
                )
            }
        }
        if (local > 0f && local < 1f) {
            val alpha = (1f - local).let { it * it }
            repeat(24) { spark ->
                val angle = spark * (PI * 2 / 24).toFloat() + random.nextFloat() * .28f
                val distance = 20f + local * (95f + random.nextFloat() * 145f)
                val point = Offset(
                    crackerCenter.x + cos(angle) * distance,
                    crackerCenter.y + sin(angle) * distance + 100f * local * local
                )
                drawLine(
                    (if (spark % 3 == 0) Color.White else festiveColors[spark % festiveColors.size])
                        .copy(alpha = alpha),
                    Offset(
                        point.x - cos(angle) * 28f,
                        point.y - sin(angle) * 28f
                    ),
                    point,
                    2.5f + (1f - local) * 4f
                )
                if (spark % 4 == 0) {
                    rotate(spark * 31f + local * 500f, point) {
                        drawRect(
                            Color(0xFFE3262E).copy(alpha = alpha),
                            topLeft = Offset(point.x - 4f, point.y - 8f),
                            size = androidx.compose.ui.geometry.Size(8f, 16f)
                        )
                    }
                }
            }
            drawCircle(Color(0xFFFFB300).copy(alpha = alpha * .35f), 70f * (1f - local), crackerCenter)
            drawCircle(Color.White.copy(alpha = alpha), 32f * (1f - local), crackerCenter)
        }
    }

    // 红纸屑持续落下。
    repeat(80) {
        val x = random.nextFloat() * size.width
        val y = (random.nextFloat() * size.height + progress * size.height * 1.35f) % size.height
        drawRect(
            if (it % 3 == 0) Color(0xFFFFD740) else Color(0xFFE3262E),
            topLeft = Offset(x, y),
            size = androidx.compose.ui.geometry.Size(5f, 12f),
            alpha = (1f - progress * .55f).coerceIn(0f, 1f)
        )
    }
}
