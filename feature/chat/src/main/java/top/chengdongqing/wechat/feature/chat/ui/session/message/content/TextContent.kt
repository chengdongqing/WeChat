package top.chengdongqing.wechat.feature.chat.ui.session.message.content

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.magnifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toUri
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.components.chat.WeMessageText
import top.chengdongqing.wechat.core.designsystem.text.parseRichText
import top.chengdongqing.wechat.core.designsystem.text.rememberEmojiInlineContent
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext
import kotlin.math.roundToInt

/**
 * 文本消息内容
 *
 * 支持富文本（URL、电话、表情）
 */
@Composable
fun TextContent(
    message: ChatMessage,
    selection: TextRange? = null,
    onSelectionChange: (TextRange) -> Unit = {},
    onSelectionDragChange: (Boolean) -> Unit = {},
    onSelectionBoundsChange: (Offset, Float) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val chatContext = LocalChatSessionContext.current
    val content = message.content as MessageContent.Text

    /* 解析富文本（URL、电话高亮+点击） */
    val annotatedString = remember(content.text) {
        content.text.parseRichText(
            onUrlClick = { url -> chatContext?.onNavigateToWebView(url) },
            onPhoneClick = { phone ->
                val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                context.startActivity(intent)
            }
        )
    }

    val inlineContent = rememberEmojiInlineContent(annotatedString, emojiSize = 22.sp)
    val colors = ChatTheme.colorScheme
    var textLayout by remember(content.text) { mutableStateOf<TextLayoutResult?>(null) }
    var textPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var magnifierPosition by remember { mutableStateOf(Offset.Unspecified) }
    var isDraggingSelection by remember { mutableStateOf(false) }
    var dragSelection by remember(content.text) { mutableStateOf<SelectionDragState?>(null) }
    val selectionStart = selection?.min?.coerceIn(0, content.text.length)
    val selectionEnd = selection?.max?.coerceIn(0, content.text.length)
    val currentOnSelectionBoundsChange by rememberUpdatedState(onSelectionBoundsChange)
    val currentOnSelectionDragChange by rememberUpdatedState(onSelectionDragChange)

    fun updateDraggingState(isDragging: Boolean) {
        isDraggingSelection = isDragging
        if (!isDragging) {
            magnifierPosition = Offset.Unspecified
            dragSelection = null
        }
        currentOnSelectionDragChange(isDragging)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isDraggingSelection) currentOnSelectionDragChange(false)
        }
    }

    LaunchedEffect(textLayout, textPositionInWindow, selectionStart, selectionEnd) {
        val layout = textLayout ?: return@LaunchedEffect
        if (selectionStart == null || selectionEnd == null || selectionStart >= selectionEnd) {
            return@LaunchedEffect
        }
        val bounds = layout.selectionLineBounds(selectionStart, selectionEnd)
        if (bounds.isNotEmpty()) {
            val left = bounds.minOf { it.left }
            val right = bounds.maxOf { it.right }
            val top = bounds.minOf { it.top }
            val bottom = bounds.maxOf { it.bottom }
            currentOnSelectionBoundsChange(
                textPositionInWindow + Offset((left + right) / 2f, top),
                bottom - top
            )
        }
    }

    Box(modifier = Modifier.padding(10.dp)) {
        WeMessageText(
            text = annotatedString,
            color = if (message.isFromMe) {
                colors.bubbleTextOutgoing
            } else {
                colors.bubbleTextIncoming
            },
            inlineContent = inlineContent,
            modifier = Modifier
                .onGloballyPositioned {
                    textPositionInWindow = it.positionInWindow()
                }
                .magnifier(
                    sourceCenter = { magnifierPosition },
                    cornerRadius = 26.dp,
                    elevation = 6.dp
                )
                .drawBehind {
                    val layout = textLayout
                    if (layout != null &&
                        selectionStart != null &&
                        selectionEnd != null &&
                        selectionStart < selectionEnd
                    ) {
                        val gap = SelectionLineGap.toPx()
                        layout.selectionLines(selectionStart, selectionEnd).forEach { line ->
                            val top = line.top + gap
                            val bottom = line.bottom - gap
                            if (bottom > top) {
                                clipRect(top = top, bottom = bottom) {
                                    drawPath(
                                        path = line.path,
                                        color = if (message.isFromMe) {
                                            colors.textSelectionBackground
                                        } else {
                                            colors.textSelectionBackground.copy(alpha = 0.28f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
            onTextLayout = { textLayout = it }
        )

        val layout = textLayout
        if (layout != null &&
            selectionStart != null &&
            selectionEnd != null &&
            selectionStart < selectionEnd
        ) {
            val activeDrag = dragSelection
            val firstOffset = activeDrag?.currentOffset ?: selectionStart
            val secondOffset = activeDrag?.fixedOffset ?: selectionEnd
            val firstIsStart = firstOffset <= secondOffset

            MessageSelectionHandle(
                position = layout.getCursorRect(firstOffset).let {
                    if (firstIsStart) it.bottomLeft else it.bottomRight
                },
                isStartHandle = firstIsStart,
                color = colors.textSelectionHandle,
                onDragStart = { position ->
                    dragSelection = SelectionDragState(
                        fixedOffset = selectionEnd,
                        currentOffset = selectionStart
                    )
                    magnifierPosition = position
                    updateDraggingState(true)
                },
                onDragTo = { position ->
                    magnifierPosition = position
                    val current = dragSelection ?: return@MessageSelectionHandle
                    val offset = layout.getOffsetForPosition(position)
                        .coerceIn(0, content.text.length)
                        .avoidCollapsingOnto(
                            anchor = current.fixedOffset,
                            previous = current.currentOffset,
                            textLength = content.text.length
                        )
                    dragSelection = current.copy(currentOffset = offset)
                    onSelectionChange(
                        TextRange(
                            minOf(offset, current.fixedOffset),
                            maxOf(offset, current.fixedOffset)
                        )
                    )
                },
                onDragEnd = { updateDraggingState(false) }
            )
            MessageSelectionHandle(
                position = layout.getCursorRect(secondOffset).let {
                    if (firstIsStart) it.bottomRight else it.bottomLeft
                },
                isStartHandle = !firstIsStart,
                color = colors.textSelectionHandle,
                onDragStart = { position ->
                    dragSelection = SelectionDragState(
                        fixedOffset = selectionStart,
                        currentOffset = selectionEnd
                    )
                    magnifierPosition = position
                    updateDraggingState(true)
                },
                onDragTo = { position ->
                    magnifierPosition = position
                    val current = dragSelection ?: return@MessageSelectionHandle
                    val offset = layout.getOffsetForPosition(position)
                        .coerceIn(0, content.text.length)
                        .avoidCollapsingOnto(
                            anchor = current.fixedOffset,
                            previous = current.currentOffset,
                            textLength = content.text.length
                        )
                    dragSelection = current.copy(currentOffset = offset)
                    onSelectionChange(
                        TextRange(
                            minOf(offset, current.fixedOffset),
                            maxOf(offset, current.fixedOffset)
                        )
                    )
                },
                onDragEnd = { updateDraggingState(false) }
            )
        }
    }
}

@Composable
private fun MessageSelectionHandle(
    position: Offset,
    isStartHandle: Boolean,
    color: Color,
    onDragStart: (Offset) -> Unit,
    onDragTo: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var dragDistance by remember { mutableStateOf(Offset.Zero) }
    var dragStartPosition by remember { mutableStateOf(position) }
    val currentPosition by rememberUpdatedState(position)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragTo by rememberUpdatedState(onDragTo)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val handleSizePx = with(LocalDensity.current) { HandleTouchSize.roundToPx() }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = position.x.roundToInt() - if (isStartHandle) handleSizePx else 0,
            y = position.y.roundToInt()
        ),
        properties = PopupProperties(clippingEnabled = false)
    ) {
        Canvas(
            modifier = Modifier
                .size(HandleTouchSize)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragDistance = Offset.Zero
                            dragStartPosition = currentPosition
                            currentOnDragStart(currentPosition)
                        },
                        onDragEnd = currentOnDragEnd,
                        onDragCancel = currentOnDragEnd,
                        onDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount
                            currentOnDragTo(dragStartPosition + dragDistance)
                        }
                    )
                }
        ) {
            val radius = 8.dp.toPx()
            val centerX = if (isStartHandle) size.width - radius else radius
            val squareLeft = if (isStartHandle) size.width - radius else 0f

            // 与系统选择手柄一致：圆形叠加一个顶部方角，起点和终点水平镜像。
            drawRect(
                color = color,
                topLeft = Offset(squareLeft, 0f),
                size = Size(radius, radius)
            )
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(centerX, radius)
            )
        }
    }
}

private fun TextLayoutResult.selectionLineBounds(start: Int, end: Int): List<Rect> {
    return selectionLines(start, end).map { it.path.getBounds() }
}

private fun TextLayoutResult.selectionLines(start: Int, end: Int): List<SelectionLine> {
    if (start >= end) return emptyList()

    val firstLine = getLineForOffset(start)
    val lastLine = getLineForOffset(end - 1)
    return (firstLine..lastLine).mapNotNull { line ->
        val lineStart = maxOf(start, getLineStart(line))
        val lineEnd = minOf(end, getLineEnd(line, visibleEnd = true))
        if (lineStart < lineEnd) {
            SelectionLine(
                path = getPathForRange(lineStart, lineEnd),
                top = getLineTop(line),
                bottom = getLineBottom(line)
            )
        } else {
            null
        }
    }
}

private data class SelectionLine(
    val path: Path,
    val top: Float,
    val bottom: Float
)

private data class SelectionDragState(
    val fixedOffset: Int,
    val currentOffset: Int
)

private fun Int.avoidCollapsingOnto(anchor: Int, previous: Int, textLength: Int): Int {
    if (this != anchor) return this
    return when {
        previous < anchor -> (anchor - 1).coerceAtLeast(0)
        previous > anchor -> (anchor + 1).coerceAtMost(textLength)
        anchor < textLength -> anchor + 1
        else -> (anchor - 1).coerceAtLeast(0)
    }
}

private val HandleTouchSize = 44.dp
private val SelectionLineGap = 1.dp
