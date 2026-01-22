package top.chengdongqing.wechat.ui.chatdetail.bottombar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import top.chengdongqing.wechat.data.sticker.Emoji
import top.chengdongqing.wechat.data.sticker.Emojis
import top.chengdongqing.wechat.ui.theme.GreenPrimary

@Composable
fun EmojiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 16,
    maxHeightDp: Dp? = 120.dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx().toInt() }
    val cursorWidthPx = with(density) { 2.dp.toPx().toInt() }
    val maxHeightPx = with(density) { maxHeightDp?.toPx()?.toInt() }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AppCompatEditText(ctx).apply {
                background = null
                isFocusable = true
                isFocusableInTouchMode = true
                textSize = fontSizeSp.toFloat()
                gravity = Gravity.TOP or Gravity.START
                includeFontPadding = false // 必须关闭，否则垂直居中会偏
                setPadding(0, 0, 0, 0)

                // 设置光标颜色
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textCursorDrawable = GradientDrawable().apply {
                        setColor(GreenPrimary.toArgb())
                        setSize(cursorWidthPx, 0)
                    }
                }

                // 限制最大高度
                maxHeightPx?.let {
                    maxHeight = it
                    isVerticalScrollBarEnabled = true
                    scrollBarStyle = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                }

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        // 只有非 update 块触发的变更才同步给外部
                        if (tag != "IGNORE_UPDATE") {
                            onValueChange(s?.toString() ?: "")
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        },
        update = { editText ->
            if (editText.maxHeight != maxHeightPx && maxHeightPx != null) {
                editText.maxHeight = maxHeightPx
            }

            // 只有当文本内容真正改变时才 setText，防止失去焦点或光标乱跳
            if (editText.text.toString() != value) {
                val start = editText.selectionStart
                val end = editText.selectionEnd

                editText.tag = "IGNORE_UPDATE"
                val spannable = EmojiSpanUtils.decode(context, value, fontSizePx)
                editText.setText(spannable)

                // 恢复光标位置
                val length = spannable.length
                editText.setSelection(start.coerceIn(0, length), end.coerceIn(0, length))
                editText.tag = null
            }
        }
    )
}

private object EmojiSpanUtils {
    // 简单的内存缓存，避免重复解码 Asset
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    fun decode(
        context: Context,
        text: CharSequence,
        fontSizePx: Int,
        emojis: List<Emoji> = Emojis
    ): Spannable {
        val spannable = SpannableStringBuilder(text)
        val pattern = Regex("\\[(.*?)]")
        val targetSize = (fontSizePx * 1.3f).toInt()

        pattern.findAll(text).toList().reversed().forEach { match ->
            val description = match.groupValues[1]
            val emoji = emojis.find { it.description == description } ?: return@forEach

            // 缓存 Key 包含尺寸，确保不同字号下清晰度正确
            val cacheKey = "${emoji.icon}_$targetSize"
            val bitmap = bitmapCache.getOrPut(cacheKey) {
                context.assets.open(emoji.icon).use { inputStream ->
                    val raw = BitmapFactory.decodeStream(inputStream)
                    Bitmap.createScaledBitmap(raw, targetSize, targetSize, true).also {
                        if (raw != it) raw.recycle() // 及时回收原图
                    }
                }
            }

            val drawable = BitmapDrawable(context.resources, bitmap).apply {
                setBounds(0, 0, targetSize, targetSize)
            }
            spannable.setSpan(
                VerticalCenterImageSpan(drawable),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }
}

private class VerticalCenterImageSpan(drawable: Drawable) : ImageSpan(drawable) {
    override fun getSize(
        paint: Paint, text: CharSequence?, start: Int, end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val rect = drawable.bounds
        if (fm != null) {
            val fontMetrics = paint.fontMetricsInt
            val fontHeight = fontMetrics.bottom - fontMetrics.top
            val drHeight = rect.bottom - rect.top

            // 如果图片比文字高，需要撑开行间距
            if (drHeight > fontHeight) {
                // 计算差值的一半
                val offset = (drHeight - fontHeight) / 2

                // 给 bottom 和 descent 留出足够的空间（多加 2-3 像素缓冲）
                fm.ascent = fontMetrics.ascent - offset
                fm.top = fontMetrics.top - offset
                fm.descent = fontMetrics.descent + offset + 2
                fm.bottom = fontMetrics.bottom + offset + 2
            }
        }
        return rect.right
    }

    override fun draw(
        canvas: Canvas, text: CharSequence?, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val b = drawable
        canvas.save()
        // 这里的计算要和 getSize 撑开的高度匹配
        // bottom - top 是当前行的总高度，b.bounds.bottom 是图片高度
        val transY = ((bottom - top) - b.bounds.bottom) / 2 + top
        canvas.translate(x, transY.toFloat())
        b.draw(canvas)
        canvas.restore()
    }
}