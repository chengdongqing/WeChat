package top.chengdongqing.wechat.feature.chat.ui.session.message.content

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.window.rememberScreenFractionWidth
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext

@Composable
fun VoiceContent(message: ChatMessage) {
    val isFromMe = message.isFromMe
    val content = message.content as MessageContent.Voice
    val chatContext = LocalChatSessionContext.current

    // 根据时长计算气泡宽度
    val currentFraction = remember(content.duration) {
        val durationSeconds = content.duration / 1000f
        val progress = (durationSeconds / 60f).coerceIn(0f, 1f)
        0.2f + (progress * 0.2f) // min 0.2f, max 0.4f
    }
    val targetWidth = rememberScreenFractionWidth(currentFraction)

    // 是否播放中
    val isPlaying by remember(message.id, chatContext?.playingMessageId) {
        derivedStateOf {
            chatContext?.playingMessageId == message.id
        }
    }

    // 时长
    val durationText = remember(content.duration) {
        "${maxOf(1, (content.duration / 1000).toInt())}''"
    }

    val colors = ChatTheme.colorScheme
    val color = if (isFromMe) colors.bubbleTextOutgoing else colors.bubbleTextIncoming

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isFromMe) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier
                .width(targetWidth)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VoiceIcon(isFromMe, isPlaying, color = color)

            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                Text(
                    text = durationText,
                    fontSize = 16.sp,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun VoiceIcon(
    isFromMe: Boolean,
    isPlaying: Boolean,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "VoiceIconTransition")
    val progress by if (isPlaying) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WaveProgress"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val maskPath = remember { Path() }
    val painter = painterResource(id = R.drawable.ic_voice_outlined)

    Box(
        modifier = Modifier
            .size(20.dp)
            .graphicsLayer(scaleX = if (isFromMe) -1f else 1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            maskPath.reset()
            val centerX = width * 0.2f
            val centerY = height / 2f

            val radius = when {
                progress < 0.33f -> width * 0.25f // 阶段1：只露小点
                progress < 0.66f -> width * 0.50f // 阶段2：露点+小波纹
                else -> width * 1.2f              // 阶段3：全露
            }

            maskPath.addOval(Rect(center = Offset(centerX, centerY), radius = radius))

            clipPath(maskPath) {
                with(painter) {
                    draw(
                        size = size,
                        colorFilter = ColorFilter.tint(color)
                    )
                }
            }
        }
    }
}