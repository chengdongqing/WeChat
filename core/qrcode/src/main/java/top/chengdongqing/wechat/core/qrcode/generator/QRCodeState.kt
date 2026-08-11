package top.chengdongqing.wechat.core.qrcode.generator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
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

    val bitMatrix = generateQrMatrix(content)
    val regions: QrCodeRegions
        get() = QrCodeRegions(bitMatrix.width, if (logoPainter != null) logoPercent else 0f)

    /**
     * 生成二维码矩阵
     * 使用H级纠错（可遮盖约30%面积），MARGIN设为0由外层padding处理静区
     */
    private fun generateQrMatrix(content: String): BitMatrix {
        val hints = mutableMapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q,
            EncodeHintType.MARGIN to 0
        )

        return QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            0, 0,
            hints
        )
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
    backgroundColor: Color = Color.Transparent,
    logoPercent: Float = 0.22f,
    dotStyle: QrDotStyle = QrDotStyle.Round
): QRCodeState = remember(content, logoPainter) {
    QRCodeState(content, logoPainter, brush, backgroundColor, logoPercent, dotStyle)
}
