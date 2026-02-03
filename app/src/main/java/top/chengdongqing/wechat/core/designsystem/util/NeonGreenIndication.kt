package top.chengdongqing.wechat.core.designsystem.util

import android.graphics.BlurMaskFilter
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
import top.chengdongqing.wechat.core.designsystem.theme.GreenPrimary

/**
 * 一个自定义的交互指示器 (Indication)，
 * 用于在组件被按下 (Pressed) 时展示霓虹绿色的外发光效果。
 */
@Stable
object NeonGreenIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return NeonGreenNode(interactionSource)
    }

    private class NeonGreenNode(
        private val interactionSource: InteractionSource
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

        // 核心绘制逻辑：决定组件在屏幕上的长相
        override fun ContentDrawScope.draw() {
            // 先绘制组件原本的内容
            drawContent()

            // 如果被按下，则在其上方覆盖一层霓虹效果
            if (isPressed) {
                // 使用底层 Canvas 绘制外发光（Compose 原生 drawRect 暂不支持 BlurMaskFilter）
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = GreenPrimary.toArgb()
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

                // 绘制最外层的实心绿色细边框，增强线条感
                drawRect(
                    color = GreenPrimary,
                    style = Stroke(width = 1f) // 描边模式
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}