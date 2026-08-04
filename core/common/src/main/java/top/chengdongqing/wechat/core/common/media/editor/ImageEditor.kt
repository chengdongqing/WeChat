package top.chengdongqing.wechat.core.common.media.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.core.graphics.withClip
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.file.createImageUri
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import kotlin.math.min
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath

private enum class EditorTool { None, Doodle, Mosaic }

private data class EditorStroke(
    val tool: EditorTool,
    val points: List<Offset>,
    val color: Color,
    val widthFraction: Float
)

@Composable
fun ImageEditor(
    sourceUri: Uri,
    onCancel: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUri by remember(sourceUri) { mutableStateOf(sourceUri) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mosaicBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tool by remember { mutableStateOf(EditorTool.None) }
    var color by remember { mutableStateOf(Color(0xFFFF3B30)) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var cropping by remember { mutableStateOf(false) }
    val strokes = remember { mutableStateListOf<EditorStroke>() }
    val redo = remember { mutableStateListOf<EditorStroke>() }

    LaunchedEffect(currentUri) {
        bitmap = withContext(Dispatchers.IO) { context.decodeBitmap(currentUri) }
        mosaicBitmap = withContext(Dispatchers.Default) { bitmap?.pixelated() }
    }
    BackHandler(onBack = onCancel)

    fun flattenThen(block: (Uri) -> Unit) {
        val source = bitmap ?: return
        if (strokes.isEmpty()) {
            block(currentUri)
            return
        }
        scope.launch {
            saving = true
            val uri = context.createImageUri(renderEdits(source, strokes.toList(), canvasSize))
            saving = false
            block(uri)
        }
    }

    if (cropping) {
        bitmap?.let { source ->
            FreeformImageCropper(
                source = source,
                onCancel = { cropping = false },
                onConfirm = { cropped ->
                    scope.launch {
                        saving = true
                        val croppedUri = context.createImageUri(cropped)
                        bitmap = cropped
                        mosaicBitmap = cropped.pixelated()
                        currentUri = croppedUri
                        strokes.clear()
                        redo.clear()
                        tool = EditorTool.None
                        cropping = false
                        saving = false
                    }
                }
            )
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(bottom = 172.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(tool, bitmap) {
                    if (tool == EditorTool.None || bitmap == null) return@pointerInput
                    detectDragGestures(
                        onDragStart = { activePoints = listOf(it) },
                        onDragEnd = {
                            if (activePoints.size > 1) {
                                strokes += EditorStroke(tool, activePoints, color, 0.012f)
                                redo.clear()
                            }
                            activePoints = emptyList()
                        },
                        onDragCancel = { activePoints = emptyList() },
                        onDrag = { change, _ ->
                            change.consume()
                            activePoints = activePoints + change.position
                        }
                    )
                }
        ) {
            bitmap?.let { source ->
                val viewport = imageViewport(source.width, source.height, size)
                drawImage(
                    image = source.asImageBitmap(),
                    dstOffset = IntOffset(
                        viewport.left.toInt(),
                        viewport.top.toInt()
                    ),
                    dstSize = IntSize(viewport.width.toInt(), viewport.height.toInt())
                )
                val allStrokes = if (activePoints.isEmpty()) strokes else strokes + EditorStroke(
                    tool, activePoints, color, 0.012f
                )
                allStrokes.forEach { stroke ->
                    drawEditorStroke(stroke, source, mosaicBitmap, viewport)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "取消",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.clickable(onClick = onCancel)
            )
            Spacer(Modifier.weight(1f))
            IconButton(enabled = strokes.isNotEmpty(), onClick = {
                redo += strokes.removeAt(strokes.lastIndex)
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    "撤销",
                    tint = if (strokes.isNotEmpty()) Color.White else Color.Gray
                )
            }
            IconButton(enabled = redo.isNotEmpty(), onClick = {
                strokes += redo.removeAt(redo.lastIndex)
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    "重做",
                    tint = if (redo.isNotEmpty()) Color.White else Color.Gray
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            if (tool == EditorTool.Doodle) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Color.White,
                        Color.Black,
                        Color(0xFFFF3B30),
                        Color(0xFFFF9500),
                        Color(0xFFFFCC00),
                        Color(0xFF34C759),
                        Color(0xFF00A9F4)
                    ).forEach { itemColor ->
                        val scale by animateFloatAsState(
                            targetValue = if (color == itemColor) 1f else 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "doodleColorScale"
                        )

                        Box(
                            Modifier
                                .size(30.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .background(itemColor, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { color = itemColor }
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditorAction(Icons.Default.Edit, "涂鸦", tool == EditorTool.Doodle) {
                        tool = if (tool == EditorTool.Doodle) EditorTool.None else EditorTool.Doodle
                    }
                    EditorAction(Icons.Default.Crop, "裁剪", false) {
                        bitmap?.let { source ->
                            scope.launch {
                                if (strokes.isNotEmpty()) {
                                    bitmap = renderEdits(source, strokes.toList(), canvasSize)
                                    mosaicBitmap = bitmap?.pixelated()
                                    strokes.clear()
                                    redo.clear()
                                }
                                cropping = true
                            }
                        }
                    }
                    EditorAction(Icons.Default.GridOn, "马赛克", tool == EditorTool.Mosaic) {
                        tool = if (tool == EditorTool.Mosaic) EditorTool.None else EditorTool.Mosaic
                    }
                }
                WeButton(
                    text = if (saving) "处理中..." else "完成",
                    enabled = bitmap != null,
                    loading = saving,
                    size = ButtonSize.Small
                ) {
                    flattenThen(onConfirm)
                }
            }
        }
    }
}

@Composable
private fun EditorAction(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick) {
        Icon(
            icon,
            label,
            tint = if (selected) WeTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

private data class ImageViewport(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

private fun imageViewport(imageWidth: Int, imageHeight: Int, canvas: Size): ImageViewport {
    val scale = min(canvas.width / imageWidth, canvas.height / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return ImageViewport((canvas.width - width) / 2f, (canvas.height - height) / 2f, width, height)
}

private fun DrawScope.drawEditorStroke(
    stroke: EditorStroke,
    source: Bitmap,
    pixelated: Bitmap?,
    viewport: ImageViewport
) {
    if (stroke.points.size < 2) return
    val path = Path().apply {
        moveTo(stroke.points.first().x, stroke.points.first().y)
        stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    val width =
        source.width.coerceAtLeast(source.height) * stroke.widthFraction * (viewport.width / source.width)
    if (stroke.tool == EditorTool.Doodle) {
        drawPath(
            path,
            stroke.color,
            style = Stroke(
                width = width,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    } else if (pixelated != null) {
        drawIntoCanvas { canvas ->
            val strokePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                style = AndroidPaint.Style.STROKE
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
                strokeWidth = width
            }
            val clip = AndroidPath().also { strokePaint.getFillPath(path.asAndroidPath(), it) }
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.clipPath(clip)
            canvas.nativeCanvas.drawBitmap(
                pixelated,
                null,
                android.graphics.RectF(
                    viewport.left,
                    viewport.top,
                    viewport.left + viewport.width,
                    viewport.top + viewport.height
                ),
                AndroidPaint().apply { isFilterBitmap = false }
            )
            canvas.nativeCanvas.restore()
        }
    }
}

private suspend fun renderEdits(
    source: Bitmap,
    strokes: List<EditorStroke>,
    editorSize: IntSize
): Bitmap = withContext(Dispatchers.Default) {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(output)
    val pixelated by lazy { source.pixelated() }
    val viewport = imageViewport(
        source.width,
        source.height,
        Size(editorSize.width.toFloat(), editorSize.height.toFloat())
    )
    strokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        val path = AndroidPath().apply {
            val first = stroke.points.first()
            moveTo(
                (first.x - viewport.left) / viewport.width * source.width,
                (first.y - viewport.top) / viewport.height * source.height
            )
            stroke.points.drop(1).forEach { point ->
                lineTo(
                    (point.x - viewport.left) / viewport.width * source.width,
                    (point.y - viewport.top) / viewport.height * source.height
                )
            }
        }
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.STROKE
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
            strokeWidth = source.width.coerceAtLeast(source.height) * stroke.widthFraction
        }
        if (stroke.tool == EditorTool.Doodle) {
            paint.color = stroke.color.toArgb()
            canvas.drawPath(path, paint)
        } else {
            canvas.withClip(path.toStrokedPath(paint)) {
                drawBitmap(pixelated, 0f, 0f, AndroidPaint().apply { isFilterBitmap = false })
            }
        }
    }
    output
}

private fun AndroidPath.toStrokedPath(paint: AndroidPaint) =
    AndroidPath().also { paint.getFillPath(this, it) }

private fun Bitmap.pixelated(): Bitmap {
    // 保留约 36 个像素块（原为 64），让马赛克块更大、遮挡更明显。
    val smallWidth = (width / 36).coerceAtLeast(1)
    val smallHeight = (height / 36).coerceAtLeast(1)
    val small = this.scale(smallWidth, smallHeight, false)
    return small.scale(width, height, false).also { if (it !== small) small.recycle() }
}

private fun Context.decodeBitmap(uri: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(
                contentResolver,
                uri
            )
        ) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    val bitmap =
        contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) ?: return null
    val orientation = contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
        }
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
