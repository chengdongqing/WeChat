package top.chengdongqing.wechat.features.chat.ui.session.message.selection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * 自定义文本选择组件
 * 提供类似微信的文本选择体验
 */
@Composable
fun CustomTextSelection(
    textLayoutResult: TextLayoutResult?,
    selection: TextRange,
    onSelectionChange: (TextRange) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingStart by remember { mutableStateOf(false) }
    var isDraggingEnd by remember { mutableStateOf(false) }

    if (textLayoutResult == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(selection) {
                detectTapGestures(
                    onTap = { onDismiss() }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selection) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val startHandleRect = getHandleRect(
                                textLayoutResult,
                                selection.start,
                                isStart = true
                            )
                            val endHandleRect = getHandleRect(
                                textLayoutResult,
                                selection.end,
                                isStart = false
                            )

                            when {
                                startHandleRect.contains(offset) -> {
                                    isDraggingStart = true
                                    isDraggingEnd = false
                                }

                                endHandleRect.contains(offset) -> {
                                    isDraggingStart = false
                                    isDraggingEnd = true
                                }
                            }
                        },
                        onDrag = { _, dragAmount ->
                            when {
                                isDraggingStart -> {
                                    val currentOffset = textLayoutResult.getBoundingBox(
                                        selection.start
                                    ).topLeft
                                    val newOffset = currentOffset + dragAmount
                                    val newPosition =
                                        textLayoutResult.getOffsetForPosition(newOffset)

                                    if (newPosition < selection.end) {
                                        onSelectionChange(
                                            TextRange(newPosition, selection.end)
                                        )
                                    }
                                }

                                isDraggingEnd -> {
                                    val currentOffset = textLayoutResult.getBoundingBox(
                                        selection.end - 1
                                    ).bottomRight
                                    val newOffset = currentOffset + dragAmount
                                    val newPosition =
                                        textLayoutResult.getOffsetForPosition(newOffset)

                                    if (newPosition > selection.start) {
                                        onSelectionChange(
                                            TextRange(selection.start, newPosition)
                                        )
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            isDraggingStart = false
                            isDraggingEnd = false
                        }
                    )
                }
        ) {
            // 绘制选择区域高亮
            drawSelectionHighlight(textLayoutResult, selection)

            // 绘制开始手柄
            drawSelectionHandle(
                textLayoutResult = textLayoutResult,
                offset = selection.start,
                isStart = true,
                isDragging = isDraggingStart
            )

            // 绘制结束手柄
            drawSelectionHandle(
                textLayoutResult = textLayoutResult,
                offset = selection.end,
                isStart = false,
                isDragging = isDraggingEnd
            )
        }
    }
}

/**
 * 绘制选择区域高亮
 */
private fun DrawScope.drawSelectionHighlight(
    textLayoutResult: TextLayoutResult,
    selection: TextRange
) {
    if (selection.collapsed) return

    val path = Path()
    val startLine = textLayoutResult.getLineForOffset(selection.start)
    val endLine = textLayoutResult.getLineForOffset(selection.end)

    for (line in startLine..endLine) {
        val lineStart = textLayoutResult.getLineStart(line)
        val lineEnd = textLayoutResult.getLineEnd(line)

        val selectionStart = max(selection.start, lineStart)
        val selectionEnd = min(selection.end, lineEnd)

        if (selectionStart < selectionEnd) {
            val startX = textLayoutResult.getHorizontalPosition(selectionStart, true)
            val endX = textLayoutResult.getHorizontalPosition(selectionEnd, true)
            val top = textLayoutResult.getLineTop(line)
            val bottom = textLayoutResult.getLineBottom(line)

            path.addRect(
                Rect(
                    left = min(startX, endX),
                    top = top,
                    right = max(startX, endX),
                    bottom = bottom
                )
            )
        }
    }

    drawPath(
        path = path,
        color = Color(0xFF3390FF).copy(alpha = 0.3f)
    )
}

/**
 * 绘制选择手柄
 */
private fun DrawScope.drawSelectionHandle(
    textLayoutResult: TextLayoutResult,
    offset: Int,
    isStart: Boolean,
    isDragging: Boolean
) {
    val boundingBox = if (isStart) {
        textLayoutResult.getBoundingBox(offset)
    } else {
        textLayoutResult.getBoundingBox((offset - 1).coerceAtLeast(0))
    }

    val handleX = if (isStart) boundingBox.left else boundingBox.right
    val handleY = if (isStart) boundingBox.top else boundingBox.bottom

    val handleRadius = 10.dp.toPx()
    val lineHeight = 20.dp.toPx()

    val color = if (isDragging) {
        Color(0xFF1890FF)
    } else {
        Color(0xFF3390FF)
    }

    // 绘制竖线
    drawLine(
        color = color,
        start = Offset(handleX, handleY),
        end = Offset(
            handleX,
            if (isStart) handleY - lineHeight else handleY + lineHeight
        ),
        strokeWidth = 2.dp.toPx()
    )

    // 绘制圆形手柄
    drawCircle(
        color = color,
        radius = handleRadius,
        center = Offset(
            handleX,
            if (isStart) handleY - lineHeight else handleY + lineHeight
        )
    )

    // 绘制内圆
    drawCircle(
        color = Color.White,
        radius = handleRadius - 2.dp.toPx(),
        center = Offset(
            handleX,
            if (isStart) handleY - lineHeight else handleY + lineHeight
        )
    )

    // 绘制外边框
    drawCircle(
        color = color,
        radius = handleRadius,
        center = Offset(
            handleX,
            if (isStart) handleY - lineHeight else handleY + lineHeight
        ),
        style = Stroke(width = 2.dp.toPx())
    )
}

/**
 * 获取手柄的触摸区域
 */
private fun getHandleRect(
    textLayoutResult: TextLayoutResult,
    offset: Int,
    isStart: Boolean
): Rect {
    val boundingBox = if (isStart) {
        textLayoutResult.getBoundingBox(offset)
    } else {
        textLayoutResult.getBoundingBox((offset - 1).coerceAtLeast(0))
    }

    val handleX = if (isStart) boundingBox.left else boundingBox.right
    val handleY = if (isStart) boundingBox.top else boundingBox.bottom

    val handleRadius = 15f
    val lineHeight = 20f

    val centerY = if (isStart) handleY - lineHeight else handleY + lineHeight

    return Rect(
        left = handleX - handleRadius,
        top = centerY - handleRadius,
        right = handleX + handleRadius,
        bottom = centerY + handleRadius
    )
}