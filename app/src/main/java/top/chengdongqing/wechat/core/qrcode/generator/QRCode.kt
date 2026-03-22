package top.chengdongqing.wechat.core.qrcode.generator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import com.google.zxing.common.BitMatrix

/**
 * 二维码组件
 */
@Composable
fun WeQRCode(
    state: QRCodeState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        drawQrCode(
            state.bitMatrix, state.regions,
            state.brush, state.dotStyle,
            state.logoPainter, state.backgroundColor
        )
    }
}

/**
 * 绘制完整二维码
 */
internal fun DrawScope.drawQrCode(
    bitMatrix: BitMatrix,
    regions: QrCodeRegions,
    brush: Brush,
    dotStyle: QrDotStyle,
    logoPainter: Painter?,
    backgroundColor: Color
) {
    val cellSize = size.width / bitMatrix.width
    val dotSize = cellSize * 0.8f

    drawRect(backgroundColor)
    drawDataDots(bitMatrix, regions, cellSize, dotSize, brush, dotStyle)
    drawFinderPatterns(bitMatrix.width, cellSize, brush)

    if (logoPainter != null) {
        drawLogo(regions, cellSize, logoPainter, backgroundColor, brush)
    }
}

/** 数据点样式 */
enum class QrDotStyle {
    /** 圆角方块 */
    Round,

    /** 圆形 */
    Circle
}

/**
 * 遍历矩阵，将所有数据点收集到一个Path中并一次绘制
 * 跳过定位码区域和logo遮挡区域
 */
private fun DrawScope.drawDataDots(
    bitMatrix: BitMatrix,
    regions: QrCodeRegions,
    cellSize: Float,
    dotSize: Float,
    brush: Brush,
    style: QrDotStyle
) {
    val path = Path()
    val padding = (cellSize - dotSize) / 2f

    for (x in 0 until bitMatrix.width) {
        for (y in 0 until bitMatrix.height) {
            if (!regions.isDataModule(x, y, bitMatrix[x, y])) continue

            val px = x * cellSize + padding
            val py = y * cellSize + padding
            val rect = Rect(px, py, px + dotSize, py + dotSize)

            when (style) {
                QrDotStyle.Round -> path.addRoundRect(
                    RoundRect(rect, CornerRadius(cellSize * 0.2f))
                )

                QrDotStyle.Circle -> path.addOval(rect)
            }
        }
    }

    drawPath(path = path, brush = brush)
}

/**
 * 绘制左上、右上、左下三个定位码
 */
private fun DrawScope.drawFinderPatterns(matrixWidth: Int, cellSize: Float, brush: Brush) {
    drawFinderPattern(0, 0, cellSize, brush)
    drawFinderPattern(matrixWidth - 7, 0, cellSize, brush)
    drawFinderPattern(0, matrixWidth - 7, cellSize, brush)
}

/**
 * 绘制单个定位码
 *
 * 结构（7x7）：
 * - 外框：占整个7x7区域的描边
 * - 内芯：居中的3x3实心块
 *
 * @param x 定位码左上角模块坐标X
 * @param y 定位码左上角模块坐标Y
 */
private fun DrawScope.drawFinderPattern(x: Int, y: Int, cellSize: Float, brush: Brush) {
    val origin = Offset(x * cellSize, y * cellSize)

    // 外框（Stroke居中绘制，需偏移半个线宽）
    drawRoundRect(
        brush = brush,
        topLeft = origin + Offset(cellSize / 2f, cellSize / 2f),
        size = Size(cellSize * 6f, cellSize * 6f),
        cornerRadius = CornerRadius(cellSize * 0.2f),
        style = Stroke(width = cellSize)
    )

    // 内芯（从第2格开始，占3x3）
    drawRoundRect(
        brush = brush,
        topLeft = origin + Offset(cellSize * 2f, cellSize * 2f),
        size = Size(cellSize * 3f, cellSize * 3f),
        cornerRadius = CornerRadius(cellSize * 0.4f)
    )
}

/**
 * 绘制中心Logo
 *
 * 层叠结构（底 → 顶）：
 * 1. 白色底板 — 与二维码数据点之间留出空白隔离
 * 2. 渐变/实色背景 — 与二维码颜色同步
 * 3. 图标 — 居中缩放到60%，保持原色
 */
private fun DrawScope.drawLogo(
    regions: QrCodeRegions,
    cellSize: Float,
    logoPainter: Painter,
    backgroundColor: Color,
    brush: Brush
) {
    val logoSize = (regions.logoEnd - regions.logoStart) * cellSize
    val logoOffset = regions.logoStart * cellSize

    // 白色底板（比logo背景每边大1个cellSize）
    drawRect(
        color = backgroundColor,
        topLeft = Offset(logoOffset - cellSize, logoOffset - cellSize),
        size = Size(logoSize + cellSize * 2f, logoSize + cellSize * 2f)
    )

    // 与二维码同色的圆角背景
    drawRoundRect(
        brush = brush,
        topLeft = Offset(logoOffset, logoOffset),
        size = Size(logoSize, logoSize),
        cornerRadius = CornerRadius(logoSize * 0.1f)
    )

    // 图标居中缩放到60%
    val iconSize = logoSize * 0.6f
    val iconOffset = (logoSize - iconSize) / 2f

    translate(logoOffset + iconOffset, logoOffset + iconOffset) {
        with(logoPainter) {
            draw(size = Size(iconSize, iconSize))
        }
    }
}

/**
 * 二维码区域判定，用于绘制时跳过特殊区域
 *
 * @param matrixSize 矩阵边长（模块数）
 * @param logoPercent logo占二维码的比例
 * @param logoMargin logo外围额外空出的格子数量
 */
internal class QrCodeRegions(
    private val matrixSize: Int,
    logoPercent: Float = 0.22f,
    private val logoMargin: Int = 1
) {
    private val finderSize = 7
    private val logoModules = (matrixSize * logoPercent).toInt()

    val logoStart = (matrixSize - logoModules) / 2
    val logoEnd = logoStart + logoModules

    /** 是否在三角定位码区域内 */
    fun isInFinderArea(x: Int, y: Int): Boolean =
        (x < finderSize && y < finderSize) ||
                (x >= matrixSize - finderSize && y < finderSize) ||
                (x < finderSize && y >= matrixSize - finderSize)

    /** 是否在logo及其间距区域内 */
    fun isInLogoArea(x: Int, y: Int): Boolean {
        return x in (logoStart - logoMargin)..(logoEnd + logoMargin) &&
                y in (logoStart - logoMargin)..(logoEnd + logoMargin)
    }

    /** 是否为需要绘制的普通数据点（排除定位码和logo区域） */
    fun isDataModule(x: Int, y: Int, hasValue: Boolean): Boolean =
        hasValue && !isInFinderArea(x, y) && !isInLogoArea(x, y)
}