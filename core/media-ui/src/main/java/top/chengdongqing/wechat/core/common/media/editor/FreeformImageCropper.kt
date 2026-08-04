package top.chengdongqing.wechat.core.common.media.editor

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

private enum class CropHandle { Move, Left, Top, Right, Bottom, TopLeft, TopRight, BottomLeft, BottomRight }

@Composable
internal fun FreeformImageCropper(
    source: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val density = LocalDensity.current
    // 框外保留较大热区，框内只占少量空间，避免抢占整体移动手势。
    val outerTouchRadius = with(density) { 40.dp.toPx() }
    val innerTouchRadius = with(density) { 14.dp.toPx() }
    val cornerTouchRadius = with(density) { 32.dp.toPx() }
    val minimumCropSize = with(density) { 72.dp.toPx() }
    val handleLength = with(density) { 18.dp.toPx() }
    val handleWidth = with(density) { 3.dp.toPx() }
    var displayed by remember(source) { mutableStateOf(source) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var cropRect by remember { mutableStateOf(Rect.Zero) }
    var resetKey by remember { mutableIntStateOf(0) }

    val viewport = remember(displayed, canvasSize) {
        cropViewport(
            displayed.width,
            displayed.height,
            Size(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        )
    }
    LaunchedEffect(displayed, canvasSize, resetKey) {
        // 初始选中整张图片，避免四周无意义地露出暗色边缘。
        if (canvasSize != IntSize.Zero) cropRect = viewport
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // 不让初始裁剪边框贴住屏幕物理边缘，避免被系统返回手势抢占。
                .padding(start = 20.dp, end = 20.dp, bottom = 154.dp)
                // 裁剪需要从屏幕边缘开始拖动，阻止左右滑动被系统返回手势抢占。
                .systemGestureExclusion()
                .onSizeChanged { canvasSize = it }
                .pointerInput(
                    displayed,
                    canvasSize,
                    outerTouchRadius,
                    innerTouchRadius,
                    cornerTouchRadius
                ) {
                    var handle: CropHandle? = null
                    detectDragGestures(
                        onDragStart = {
                            handle = cropRect.hitHandle(
                                point = it,
                                outerRadius = outerTouchRadius,
                                innerRadius = innerTouchRadius,
                                cornerRadius = cornerTouchRadius
                            )
                        },
                        onDragEnd = { handle = null },
                        onDragCancel = { handle = null }
                    ) { change, drag ->
                        change.consume()
                        handle?.let {
                            cropRect = cropRect.drag(it, drag, viewport, minimumCropSize)
                        }
                    }
                }
        ) {
            drawImage(
                displayed.asImageBitmap(),
                dstOffset = IntOffset(viewport.left.roundToInt(), viewport.top.roundToInt()),
                dstSize = IntSize(viewport.width.roundToInt(), viewport.height.roundToInt())
            )

            val shade = Color.Black.copy(alpha = 0.58f)
            drawRect(
                shade,
                Offset(viewport.left, viewport.top),
                Size(viewport.width, cropRect.top - viewport.top)
            )
            drawRect(
                shade,
                Offset(viewport.left, cropRect.bottom),
                Size(viewport.width, viewport.bottom - cropRect.bottom)
            )
            drawRect(
                shade,
                Offset(viewport.left, cropRect.top),
                Size(cropRect.left - viewport.left, cropRect.height)
            )
            drawRect(
                shade,
                Offset(cropRect.right, cropRect.top),
                Size(viewport.right - cropRect.right, cropRect.height)
            )

            val grid = Color.White.copy(alpha = 0.72f)
            drawRect(Color.White, cropRect.topLeft, cropRect.size, style = Stroke(1.5f))
            for (part in 1..2) {
                val x = cropRect.left + cropRect.width * part / 3f
                val y = cropRect.top + cropRect.height * part / 3f
                drawLine(grid, Offset(x, cropRect.top), Offset(x, cropRect.bottom), 1f)
                drawLine(grid, Offset(cropRect.left, y), Offset(cropRect.right, y), 1f)
            }

            val handleColor = Color.White
            listOf(
                cropRect.topLeft,
                cropRect.topRight,
                cropRect.bottomLeft,
                cropRect.bottomRight
            ).forEachIndexed { index, p ->
                val horizontalEnd = if (index % 2 == 0) p.x + handleLength else p.x - handleLength
                val verticalEnd = if (index < 2) p.y + handleLength else p.y - handleLength
                drawLine(handleColor, p, Offset(horizontalEnd, p.y), handleWidth, StrokeCap.Square)
                drawLine(handleColor, p, Offset(p.x, verticalEnd), handleWidth, StrokeCap.Square)
            }
            drawLine(
                handleColor,
                Offset(cropRect.center.x - 18f, cropRect.top),
                Offset(cropRect.center.x + 18f, cropRect.top),
                handleWidth
            )
            drawLine(
                handleColor,
                Offset(cropRect.center.x - 18f, cropRect.bottom),
                Offset(cropRect.center.x + 18f, cropRect.bottom),
                handleWidth
            )
            drawLine(
                handleColor,
                Offset(cropRect.left, cropRect.center.y - 18f),
                Offset(cropRect.left, cropRect.center.y + 18f),
                handleWidth
            )
            drawLine(
                handleColor,
                Offset(cropRect.right, cropRect.center.y - 18f),
                Offset(cropRect.right, cropRect.center.y + 18f),
                handleWidth
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    displayed = displayed.rotateCounterClockwise()
                }) {
                    Icon(
                        Icons.Default.Rotate90DegreesCcw,
                        "旋转",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("还原", color = Color.White, fontSize = 17.sp, modifier = Modifier.clickable {
                    displayed = source
                    resetKey++
                })
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        "取消裁剪",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { onConfirm(displayed.crop(cropRect, viewport)) }) {
                    Icon(
                        Icons.Default.Check,
                        "确认裁剪",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

private fun cropViewport(imageWidth: Int, imageHeight: Int, canvas: Size): Rect {
    if (canvas.width <= 0f || canvas.height <= 0f) return Rect.Zero
    val scale = min(canvas.width / imageWidth, canvas.height / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return Rect(
        (canvas.width - width) / 2f,
        (canvas.height - height) / 2f,
        (canvas.width + width) / 2f,
        (canvas.height + height) / 2f
    )
}

private fun Rect.hitHandle(
    point: Offset,
    outerRadius: Float,
    innerRadius: Float,
    cornerRadius: Float
): CropHandle? {
    // 角点优先；边框热区主要向外扩展，框内大部分区域保留给 Move。
    val effectiveCornerRadius = min(cornerRadius, min(width, height) / 4f)
    fun near(a: Offset) = (point - a).getDistance() <= effectiveCornerRadius
    return when {
        near(topLeft) -> CropHandle.TopLeft
        near(topRight) -> CropHandle.TopRight
        near(bottomLeft) -> CropHandle.BottomLeft
        near(bottomRight) -> CropHandle.BottomRight
        point.x in (left - outerRadius)..(left + innerRadius) &&
                point.y in (top + effectiveCornerRadius)..(bottom - effectiveCornerRadius) -> CropHandle.Left

        point.x in (right - innerRadius)..(right + outerRadius) &&
                point.y in (top + effectiveCornerRadius)..(bottom - effectiveCornerRadius) -> CropHandle.Right

        point.y in (top - outerRadius)..(top + innerRadius) &&
                point.x in (left + effectiveCornerRadius)..(right - effectiveCornerRadius) -> CropHandle.Top

        point.y in (bottom - innerRadius)..(bottom + outerRadius) &&
                point.x in (left + effectiveCornerRadius)..(right - effectiveCornerRadius) -> CropHandle.Bottom

        contains(point) -> CropHandle.Move
        else -> null
    }
}

private fun Rect.drag(
    handle: CropHandle,
    delta: Offset,
    bounds: Rect,
    minimum: Float
): Rect {
    var l = left
    var t = top
    var r = right
    var b = bottom
    if (handle == CropHandle.Move) {
        val dx = delta.x.coerceIn(bounds.left - l, bounds.right - r)
        val dy = delta.y.coerceIn(bounds.top - t, bounds.bottom - b)
        return translate(Offset(dx, dy))
    }
    if (handle in listOf(CropHandle.Left, CropHandle.TopLeft, CropHandle.BottomLeft)) l =
        (l + delta.x).coerceIn(bounds.left, r - minimum)
    if (handle in listOf(CropHandle.Right, CropHandle.TopRight, CropHandle.BottomRight)) r =
        (r + delta.x).coerceIn(l + minimum, bounds.right)
    if (handle in listOf(CropHandle.Top, CropHandle.TopLeft, CropHandle.TopRight)) t =
        (t + delta.y).coerceIn(bounds.top, b - minimum)
    if (handle in listOf(CropHandle.Bottom, CropHandle.BottomLeft, CropHandle.BottomRight)) b =
        (b + delta.y).coerceIn(t + minimum, bounds.bottom)
    return Rect(l, t, r, b)
}

private fun Bitmap.crop(rect: Rect, viewport: Rect): Bitmap {
    val left =
        ((rect.left - viewport.left) / viewport.width * width).roundToInt().coerceIn(0, width - 1)
    val top =
        ((rect.top - viewport.top) / viewport.height * height).roundToInt().coerceIn(0, height - 1)
    val right = ((rect.right - viewport.left) / viewport.width * width).roundToInt()
        .coerceIn(left + 1, width)
    val bottom = ((rect.bottom - viewport.top) / viewport.height * height).roundToInt()
        .coerceIn(top + 1, height)
    return Bitmap.createBitmap(this, left, top, right - left, bottom - top)
}

private fun Bitmap.rotateCounterClockwise(): Bitmap {
    val matrix = Matrix().apply { postRotate(-90f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
