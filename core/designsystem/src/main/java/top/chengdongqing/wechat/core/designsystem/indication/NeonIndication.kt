package top.chengdongqing.wechat.core.designsystem.indication

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.IndicationNodeFactory
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
 * Creates an indication that draws a neon outline while the component is pressed.
 */
@Stable
fun neon(color: Color): IndicationNodeFactory = NeonNodeFactory(color)

@Stable
private class NeonNodeFactory(
    private val color: Color
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        NeonNode(interactionSource, color)

    override fun equals(other: Any?): Boolean =
        this === other || other is NeonNodeFactory && color == other.color

    override fun hashCode(): Int = color.hashCode()
}

private class NeonNode(
    private val interactionSource: InteractionSource,
    private val color: Color
) : Modifier.Node(), DrawModifierNode {
    private var isPressed by mutableStateOf(false)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                isPressed = when (interaction) {
                    is PressInteraction.Press -> true
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> false

                    else -> isPressed
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!isPressed) return

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                this.color = this@NeonNode.color.toArgb()
                maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)
            }
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
        drawRect(color = color, style = Stroke(width = 1f))
    }
}
