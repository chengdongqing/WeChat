package top.chengdongqing.wechat.ui.chatdetail.bottombar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
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
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import top.chengdongqing.wechat.data.sticker.Emoji
import top.chengdongqing.wechat.data.sticker.Emojis
import top.chengdongqing.wechat.ui.theme.GreenPrimary
import java.lang.ref.WeakReference

@Composable
fun EmojiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: NativeFocusRequester,
    fontSizeSp: Int = 16,
    maxHeightDp: Dp? = 120.dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx().toInt() }
    val cursorWidthPx = with(density) { 1.8.dp.toPx().toInt() }
    val maxHeightPx = with(density) { maxHeightDp?.toPx()?.toInt() }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            AppCompatEditText(ctx).apply {
                background = null
                textSize = fontSizeSp.toFloat()
                gravity = Gravity.TOP or Gravity.START
                includeFontPadding = false // 必须关闭，否则垂直居中会偏
                isCursorVisible = true
                isFocusable = true
                isFocusableInTouchMode = true
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
                }
                // 绑定聚焦管理器
                focusRequester.bind(this)

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
                val oldText = editText.text.toString()
                val oldStart = editText.selectionStart

                editText.tag = "IGNORE_UPDATE"
                val spannable = EmojiSpanUtils.decode(context, value, fontSizePx)
                editText.setText(spannable)

                // --- 核心修正点 ---
                // 如果新文本长度增加了，且光标原本在末尾，我们需要让光标跳到新文本的末尾
                // 或者计算出差值偏移量
                val newLength = spannable.length
                val lengthDelta = newLength - oldText.length

                // 如果是插入操作（长度增加），光标应该向后偏移
                val newSelection = if (lengthDelta > 0 && oldStart >= oldText.length) {
                    newLength // 强制跳转到末尾
                } else {
                    // 如果是在中间插入，按比例或偏移量计算，这里简单处理：保持相对位置
                    oldStart.coerceIn(0, newLength)
                }

                editText.setSelection(newSelection)
                // ------------------

                editText.tag = null
            }

            focusRequester.bind(editText)
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
                    raw.scale(targetSize, targetSize).also {
                        if (raw != it) raw.recycle() // 及时回收原图
                    }
                }
            }

            val drawable = bitmap.toDrawable(context.resources).apply {
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
        val fm = paint.fontMetricsInt
        // 基于文字的 Baseline (y) 进行偏移，确保与文字对齐，不干扰系统对行高的判断
        val transY = (y + fm.descent + y + fm.ascent) / 2 - b.bounds.bottom / 2

        canvas.withTranslation(x, transY.toFloat()) {
            b.draw(this)
        }
    }
}

/**
 * 对接原生输入框的焦点管理器
 */
class NativeFocusRequester {
    private var editTextRef: WeakReference<AppCompatEditText>? = null

    internal fun bind(editText: AppCompatEditText) {
        editTextRef = WeakReference(editText)
    }

    val selectionStart: Int
        get() = editTextRef?.get()?.selectionStart ?: 0

    fun setSelection(index: Int) {
        editTextRef?.get()?.setSelection(index)
    }

    fun requestFocus(showKeyboard: Boolean = true) {
        editTextRef?.get()?.let { view ->
            view.requestFocus()

            if (showKeyboard) {
                val imm =
                    view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun clearFocus() {
        editTextRef?.get()?.clearFocus()
    }

    fun post(action: () -> Unit) {
        editTextRef?.get()?.post { action() }
    }
}