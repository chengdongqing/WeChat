package top.chengdongqing.wechat.ui.chat.session.input.voice

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 录音遮罩层内容
 */
@Composable
fun RecordingOverlay(
    recordState: RecordState,
    audioAmplitude: Float
) {
    // 底部弧形背景
    BottomArcBackground()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 声纹气泡
        if (recordState == RecordState.Recording) {
            VoiceWaveformBubble(audioAmplitude)
            Spacer(modifier = Modifier.height(100.dp))
        }

        // 操作按钮
        ActionButtons(recordState)

        Spacer(modifier = Modifier.height(60.dp))

        // 底部提示
        BottomHintText(recordState)
    }
}

/**
 * 底部弧形背景
 */
@Composable
private fun BottomArcBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.width * 1.2f
        drawCircle(
            color = Color(0xFFF7F7F7),
            radius = radius,
            center = Offset(size.width / 2, size.height + radius * 0.7f)
        )
    }
}

/**
 * 操作按钮行（取消/转文字）
 */
@Composable
private fun ActionButtons(recordState: RecordState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            label = "取 消",
            isActive = recordState == RecordState.Cancel,
            activeColor = Color(0xFFFF3B30),
            activeTextColor = Color.White
        )
        ActionButton(
            label = "文 字",
            isActive = recordState == RecordState.Convert,
            activeColor = Color(0xFFD8D8D8),
            activeTextColor = Color.Black
        )
    }
}

/**
 * 单个操作按钮
 *
 * 动画效果：
 * - 激活时放大到80dp
 * - 未激活时64dp
 */
@Composable
private fun ActionButton(
    label: String,
    isActive: Boolean,
    activeColor: Color = Color(0xFFD8D8D8),
    activeTextColor: Color = Color.Black
) {
    val size by animateDpAsState(
        targetValue = if (isActive) 80.dp else 64.dp,
        label = "ButtonSize"
    )

    val bgColor = if (isActive) activeColor else Color(0xFFE9E9E9)
    val textColor = if (isActive) activeTextColor else Color.Black

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1),
                fontSize = 20.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/**
 * 底部提示文字
 */
@Composable
private fun BottomHintText(recordState: RecordState) {
    Text(
        text = when (recordState) {
            RecordState.Cancel -> "松开手指，取消发送"
            RecordState.Convert -> "松开手指，转文字"
            else -> "松开 发送"
        },
        color = Color.Gray,
        fontSize = 14.sp
    )
}

/**
 * 声纹气泡
 *
 * 可视化原理：
 * 1. 创建12个垂直条
 * 2. 中心条最高，两边递减（通过distance计算权重）
 * 3. 每个条的高度 = amplitude × scale × 50dp
 * 4. 使用animateFloatAsState实现平滑过渡
 * 5. 最小高度6dp确保始终可见
 */
@Composable
private fun VoiceWaveformBubble(amplitude: Float) {
    Box(
        modifier = Modifier
            .size(width = 150.dp, height = 90.dp)
            .graphicsLayer {
                shadowElevation = 4.dp.toPx()
                shape = VoiceBubbleShape()
                clip = true
            }
            .background(Color(0xFF95EC69)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(12) { index ->
                VoiceBar(
                    index = index,
                    amplitude = amplitude
                )
            }
        }
    }
}

/**
 * 单个声纹条
 *
 * 高度计算：
 * 1. 计算距离中心的距离：dist = |index - center|
 * 2. 归一化距离：normalizedDist = dist / totalBars
 * 3. 计算缩放因子：scale = 1 - normalizedDist (中心为1，边缘接近0)
 * 4. 最终高度 = amplitude × scale × 50dp
 * 5. 限制最小高度6dp
 */
@Composable
private fun VoiceBar(
    index: Int,
    totalBars: Int = 12,
    amplitude: Float
) {
    val center = (totalBars - 1) / 2f
    val distance = abs(index - center)
    val normalizedDistance = distance / totalBars
    val scale = (1f - normalizedDistance).coerceAtLeast(0.2f)

    // 计算目标高度
    val targetHeight = (amplitude * scale * 50).coerceAtLeast(6f)

    // 平滑动画过渡
    val animatedHeight by animateFloatAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BarHeight_$index"
    )

    Box(
        modifier = Modifier
            .width(3.dp)
            .height(animatedHeight.dp)
            .clip(CircleShape)
            .background(Color(0xFF191919))
    )
}

/**
 * 气泡形状
 */
private class VoiceBubbleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val cornerRadius = with(density) { 16.dp.toPx() }
            val arrowWidth = with(density) { 16.dp.toPx() }
            val arrowHeight = with(density) { 8.dp.toPx() }

            // 主体圆角矩形
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height - arrowHeight,
                    cornerRadius = CornerRadius(cornerRadius)
                )
            )

            // 底部三角尖角
            moveTo(size.width / 2 - arrowWidth / 2, size.height - arrowHeight)
            lineTo(size.width / 2, size.height)
            lineTo(size.width / 2 + arrowWidth / 2, size.height - arrowHeight)
            close()
        }
        return Outline.Generic(path)
    }
}