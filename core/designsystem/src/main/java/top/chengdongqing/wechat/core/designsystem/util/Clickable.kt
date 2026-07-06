package top.chengdongqing.wechat.core.designsystem.util

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/**
 * 没有视觉反馈的点击事件
 */
fun Modifier.onTap(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = then(
    Modifier.clickable(
        interactionSource = null,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
)

/**
 * 一个自定义的交互指示器
 * 用于在组件被按下时展示霓虹灯效果
 */
@Stable
fun neonIndication(color: Color): IndicationNodeFactory = NeonNodeFactory(color)

@Stable
private class NeonNodeFactory(
    private val color: Color
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return NeonNode(interactionSource, color)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NeonNodeFactory) return false
        return color == other.color
    }

    override fun hashCode(): Int = System.identityHashCode(this)
}

private class NeonNode(
    private val interactionSource: InteractionSource,
    private val neonColor: Color
) : Modifier.Node(), DrawModifierNode {
    private var isPressed by mutableStateOf(false)

    // 当节点挂载到 UI 树时执行
    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> isPressed = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        // 先绘制组件原本的内容
        drawContent()

        // 如果被按下，则在其上方覆盖一层霓虹效果
        if (isPressed) {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = neonColor.toArgb()
                    // 设置原生 Paint 的模糊滤镜，实现“发光”感
                    // 10f 是模糊半径，OUTER 表示只在形状外部模糊
                    maskFilter = BlurMaskFilter(
                        10f,
                        BlurMaskFilter.Blur.OUTER
                    )
                }
                // 绘制一个与组件大小一致的底层矩形，由滤镜产生外扩发光效果
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
            }

            // 绘制最外层的实心细边框，增强线条感
            drawRect(
                color = neonColor,
                style = Stroke(width = 1f) // 描边模式
            )
        }
    }
}