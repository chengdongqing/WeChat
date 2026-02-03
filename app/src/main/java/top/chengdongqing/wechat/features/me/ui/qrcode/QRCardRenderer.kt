package top.chengdongqing.wechat.features.me.ui.qrcode

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.QRCodeState
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.drawQrCode
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.data.model.UserProfile

/**
 * 二维码卡片渲染器
 *
 * 使用 CanvasDrawScope 直接绘制整张卡片到 Bitmap，
 *
 * @param profile 用户信息
 * @param state 二维码状态，提供绘制能力
 * @param avatarBitmap 头像 Bitmap
 * @param textMeasurer 文本测量器，用于计算文字尺寸和绘制
 */
class QrCardRenderer(
    private val profile: UserProfile,
    private val state: QRCodeState,
    private val avatarBitmap: Bitmap,
    private val textMeasurer: TextMeasurer
) {
    /**
     * 生成卡片 Bitmap
     *
     * @param widthPx 卡片宽度(px)，高度按 1:1.366 比例自动计算
     * @param density 屏幕密度，用于文字渲染
     */
    suspend fun generateBitmap(widthPx: Int = 1074, density: Density): Bitmap =
        withContext(Dispatchers.IO) {
            val layout = CardLayout(widthPx)

            val imageBitmap = ImageBitmap(widthPx, layout.totalHeight)
            val canvas = Canvas(imageBitmap)

            CanvasDrawScope().draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = canvas,
                size = Size(widthPx.toFloat(), layout.totalHeight.toFloat())
            ) {
                drawCard(layout, density)
            }

            imageBitmap.asAndroidBitmap()
        }

    /** 绘制整张卡片 */
    private fun DrawScope.drawCard(layout: CardLayout, density: Density) {
        drawRect(Color(0xFFFFFFFF))

        translate(left = layout.paddingHorizontal) {
            translate(top = layout.profileBarTop) {
                drawProfileBar(layout)
            }
            translate(top = layout.qrCodeTop) {
                drawQrCodeSection(layout, density)
            }
            translate(top = layout.hintTop) {
                drawHintText(layout)
            }
        }
    }

    /** 绘制头像 + 名字 + 签名 */
    private fun DrawScope.drawProfileBar(layout: CardLayout) {
        clipPath(
            Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, 0f, layout.avatarSize, layout.avatarSize),
                        cornerRadius = CornerRadius(layout.avatarCornerRadius)
                    )
                )
            }
        ) {
            drawImage(
                image = avatarBitmap.asImageBitmap(),
                dstSize = IntSize(layout.avatarSize.toInt(), layout.avatarSize.toInt())
            )
        }

        val textX = layout.avatarSize + layout.gap
        drawText(
            textMeasurer = textMeasurer,
            text = profile.nickname,
            topLeft = Offset(textX, layout.avatarSize * 0.08f),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xE6000000)
            )
        )
        if (profile.signature?.isNotBlank().isTrue()) {
            drawText(
                textMeasurer = textMeasurer,
                text = profile.signature!!,
                topLeft = Offset(textX, layout.avatarSize * 0.62f),
                style = TextStyle(
                    fontSize = 10.sp,
                    color = Color(0x80000000)
                )
            )
        }
    }

    /**
     * 绘制二维码区域
     *
     * 用嵌套 CanvasDrawScope 给 qrState.draw() 提供精确的 contentWidth × contentWidth
     * 的绘制空间，避免用 scale 缩放导致尺寸叠加
     */
    private fun DrawScope.drawQrCodeSection(layout: CardLayout, density: Density) {
        val qrSize = layout.qrCodeSize
        drawIntoCanvas { canvas ->
            CanvasDrawScope().draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = canvas,
                size = Size(qrSize, qrSize)
            ) {
                drawQrCode(
                    state.bitMatrix, state.regions,
                    state.brush, state.dotStyle,
                    state.logoPainter, state.backgroundColor
                )
            }
        }
    }

    /** 绘制居中提示语 */
    private fun DrawScope.drawHintText(layout: CardLayout) {
        val text = "扫一扫上面的二维码图案，加我为朋友。"
        val measured = textMeasurer.measure(
            text,
            style = TextStyle(fontSize = 12.sp)
        )

        drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = Offset((layout.contentWidth - measured.size.width) / 2f, 0f),
            style = TextStyle(
                fontSize = 12.sp,
                color = Color(0x80000000)
            )
        )
    }
}

/**
 * 卡片布局计算
 *
 * 分两类尺寸：
 * - 随内容缩放的：头像、间隙、二维码等，基于 scale 计算
 * - 固定的：左右边距、顶部间距，用 px 直接指定，避免随宽度放大
 *
 * @param widthPx 卡片输出宽度(px)
 */
private class CardLayout(widthPx: Int) {
    private val scale = widthPx.toFloat() / BASE_WIDTH_DP

    // 边距（固定px）
    val paddingHorizontal = 40f * scale
    val contentWidth = widthPx.toFloat() - paddingHorizontal * 2f

    // 头像
    val avatarSize = 36f * scale
    val avatarCornerRadius = 4f * scale
    val gap = 10f * scale

    val totalHeight = (widthPx * TARGET_ASPECT_RATIO).toInt()

    // 各区域top（顶部间距固定，其余随内容累加）
    val profileBarTop = 120f
    val qrCodeTop = profileBarTop + avatarSize + 28f * scale
    val qrCodeSize = contentWidth
    val hintTop = qrCodeTop + qrCodeSize + 28f * scale

    companion object {
        const val BASE_WIDTH_DP = 280f

        const val TARGET_ASPECT_RATIO = 1.366f
    }
}