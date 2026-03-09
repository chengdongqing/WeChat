package top.chengdongqing.wechat.features.contacts.ui.add.nfc.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.GreenPrimary
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun NfcWaiting(isReaderMode: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    ) {
        PulsingNfcIcon()
        Spacer(Modifier.height(52.dp))
        Text(
            text = "将手机和对方手机背靠背",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = WeTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = isReaderMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
                    .using(
                        SizeTransform(clip = false)
                    )
            }
        ) { isReaderMode ->
            Text(
                text = buildString {
                    append("碰触后将自动拉取对方信息\n")
                    if (!isReaderMode) {
                        append("部分手机需要手动设置 NFC 为 HCE 卡模拟模式")
                    }
                },
                fontSize = 14.sp,
                color = WeTheme.colorScheme.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun PulsingNfcIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val rings = remember {
        listOf(
            RingConfig(delayMs = 0, sizeDp = 140f, maxAlpha = 0.50f),
            RingConfig(delayMs = 450, sizeDp = 190f, maxAlpha = 0.28f),
            RingConfig(delayMs = 900, sizeDp = 245f, maxAlpha = 0.12f)
        )
    }

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
                    .border(width = 1.5.dp, color = GreenPrimary, shape = CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(GreenPrimary.copy(alpha = 0.20f), GreenPrimary.copy(alpha = 0.06f))
                    )
                )
                .border(2.dp, GreenPrimary.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nfc_outlined),
                contentDescription = "NFC",
                tint = GreenPrimary,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Immutable
private data class RingConfig(
    val delayMs: Int,
    val sizeDp: Float,
    val maxAlpha: Float
)