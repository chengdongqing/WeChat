package top.chengdongqing.wechat.core.designsystem.components.qrcode.generator

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.designsystem.theme.Black

/**
 * 二维码组件状态
 *
 * 持有二维码所有配置参数，同时暴露生成Bitmap的能力
 * 确保组件预览和保存图片使用完全一致的参数
 *
 * @param content 编码内容
 * @param logoPainter logo图标，为null时不显示
 * @param brush 二维码颜色，支持SolidColor和Gradient
 * @param backgroundColor 背景颜色
 * @param logoPercent logo占比，不建议超过0.25（H级纠错上限约30%）
 * @param dotStyle 数据点样式
 */
@Stable
class QRCodeState(
    val content: String,
    val logoPainter: Painter? = null,
    brush: Brush = SolidColor(Black),
    backgroundColor: Color = Color.White,
    logoPercent: Float = 0.22f,
    dotStyle: QrDotStyle = QrDotStyle.Round
) {
    var brush: Brush by mutableStateOf(brush)
    var backgroundColor: Color by mutableStateOf(backgroundColor)
    var logoPercent: Float by mutableFloatStateOf(logoPercent)
    var dotStyle: QrDotStyle by mutableStateOf(dotStyle)

    // 内部缓存，避免重复生成
    internal val bitMatrix = generateQrMatrix(content)
    internal val regions: QrCodeRegions
        get() = QrCodeRegions(bitMatrix.width, if (logoPainter != null) logoPercent else 0f)

    /**
     * 生成二维码Bitmap
     *
     * @param sizePx 图片边长(px)，建议至少600px
     * @param density 屏幕密度
     */
    suspend fun generateBitmap(sizePx: Int = 1024, density: Density): Bitmap =
        withContext(Dispatchers.IO) {
            val imageBitmap = ImageBitmap(sizePx, sizePx)
            val canvas = Canvas(imageBitmap)

            CanvasDrawScope().draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = canvas,
                size = Size(sizePx.toFloat(), sizePx.toFloat())
            ) {
                drawQrCode(bitMatrix, regions, brush, dotStyle, logoPainter, backgroundColor)
            }

            imageBitmap.asAndroidBitmap()
        }
}

/**
 * 创建并remember QRCodeState
 *
 * @param content 编码内容
 * @param logoPainter logo图标
 * @param brush 初始颜色
 * @param backgroundColor 初始背景色
 * @param logoPercent 初始logo占比
 * @param dotStyle 初始数据点样式
 */
@Composable
fun rememberQRCodeState(
    content: String,
    logoPainter: Painter? = null,
    brush: Brush = SolidColor(Black),
    backgroundColor: Color = Color.White,
    logoPercent: Float = 0.22f,
    dotStyle: QrDotStyle = QrDotStyle.Round
): QRCodeState = remember(content, logoPainter) {
    QRCodeState(content, logoPainter, brush, backgroundColor, logoPercent, dotStyle)
}