package top.chengdongqing.wechat.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

@Stable
object NeonGreenIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return NeonGreenNode(interactionSource)
    }

    private class NeonGreenNode(
        private val interactionSource: InteractionSource
    ) : Modifier.Node(), DrawModifierNode {
        private var isPressed by mutableStateOf(false)

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
            drawContent()

            if (isPressed) {
                // 绘制外发光
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = GreenPrimary.toArgb()
                        // 设置发光模糊
                        maskFilter = android.graphics.BlurMaskFilter(
                            10f,
                            android.graphics.BlurMaskFilter.Blur.OUTER
                        )
                    }
                    // 绘制一个带模糊的矩形边框
                    canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                }
                // 绘制实心绿色边框
                drawRect(color = GreenPrimary, style = Stroke(width = 1f))
            }
        }
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}