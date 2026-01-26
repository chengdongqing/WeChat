package top.chengdongqing.wechat.ui.chat.session.input

import android.content.Context
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
import androidx.core.graphics.withTranslation
import top.chengdongqing.wechat.ui.theme.GreenPrimary
import top.chengdongqing.wechat.ui.utils.EmojiManager
import top.chengdongqing.wechat.ui.utils.NativeFocusRequester

@Composable
fun EmojiTextField(
    value: String,
    modifier: Modifier = Modifier,
    focusRequester: NativeFocusRequester,
    fontSizeSp: Int = 16,
    maxHeightDp: Dp? = 120.dp,
    onValueChange: (String) -> Unit,
    onLineCountChange: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // 预计算像素值，避免在 AndroidView 中频繁计算
    val fontSizePx = with(density) { fontSizeSp.sp.toPx().toInt() }
    val cursorWidthPx = with(density) { 1.8.dp.toPx().toInt() }
    val maxHeightPx = with(density) { maxHeightDp?.toPx()?.toInt() }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AppCompatEditText(ctx).apply {
                setupConfig(fontSizeSp, cursorWidthPx, maxHeightPx)

                // 绑定聚焦
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
                        // 屏蔽由 update 回调引起的 Text 变更同步
                        if (tag != TAG_IGNORE_UPDATE) {
                            onValueChange(s?.toString() ?: "")
                            // 初始化行数
                            post { onLineCountChange?.invoke(lineCount) }
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        },
        update = { editText ->
            // 只有当参数变化时才触发布局更新
            if (editText.maxHeight != (maxHeightPx ?: Int.MAX_VALUE)) {
                editText.maxHeight = maxHeightPx ?: Int.MAX_VALUE
            }

            if (editText.text.toString() != value) {
                updateTextWithEmoji(editText, context, value, fontSizePx)
                // 更新行数
                editText.post { onLineCountChange?.invoke(editText.lineCount) }
            }

            focusRequester.bind(editText)
        }
    )
}

private const val TAG_IGNORE_UPDATE = "IGNORE_UPDATE"

/**
 * EditText初始化配置
 */
private fun AppCompatEditText.setupConfig(fontSizeSp: Int, cursorWidth: Int, maxH: Int?) {
    background = null
    textSize = fontSizeSp.toFloat()
    gravity = Gravity.TOP or Gravity.START
    includeFontPadding = false
    isFocusableInTouchMode = true
    setPadding(0, 0, 0, 0)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        textCursorDrawable = GradientDrawable().apply {
            setColor(GreenPrimary.toArgb())
            setSize(cursorWidth, 0)
        }
    }
    maxH?.let { maxHeight = it }
}

/**
 * 文本更新与光标保持逻辑
 */
private fun updateTextWithEmoji(
    editText: AppCompatEditText,
    context: Context,
    value: String,
    sizePx: Int
) {
    val oldStart = editText.selectionStart
    val oldLength = editText.text?.length ?: 0

    editText.tag = TAG_IGNORE_UPDATE
    val spannable = context.decodeEmojiSpan(value, sizePx)
    editText.setText(spannable)

    // 若文本增长且光标在末尾，则跟进；否则保持相对位置
    val newLength = spannable.length
    val newSelection = if (newLength > oldLength && oldStart >= oldLength) {
        newLength
    } else {
        oldStart.coerceIn(0, newLength)
    }
    editText.setSelection(newSelection)
    editText.tag = null
}

/**
 * 解析表情并占位
 */
private fun Context.decodeEmojiSpan(text: CharSequence, fontSizePx: Int): Spannable {
    val spannable = SpannableStringBuilder(text)
    val targetSize = (fontSizePx * 1.3f).toInt()

    EmojiManager.findAllMatches(text).reversed().forEach { (emoji, range) ->
        val bitmap = EmojiManager.getEmojiBitmap(this, emoji, targetSize)
        val drawable = bitmap.toDrawable(resources).apply {
            setBounds(0, 0, targetSize, targetSize)
        }
        spannable.setSpan(
            VerticalCenterImageSpan(drawable),
            range.first,
            range.last + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return spannable
}

/**
 * 垂直居中的 ImageSpan，确保表情在文字行内居中。
 * 解决了 ImageSpan 默认对齐 Baseline 或 Bottom 导致的视觉偏移。
 */
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

            if (drHeight > fontHeight) {
                // 当图片高度超过文字行高时，上下对称扩展 FontMetrics
                val offset = (drHeight - fontHeight) / 2
                fm.ascent = fontMetrics.ascent - offset
                fm.top = fontMetrics.top - offset
                fm.descent = fontMetrics.descent + offset + 2 // 增加微小缓冲防止切断
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
        // 计算公式：Baseline + (Descent + Ascent) / 2 = 文本中心点，再减去图片高度的一半
        val transY = (y + fm.descent + y + fm.ascent) / 2f - b.bounds.bottom / 2f

        canvas.withTranslation(x, transY) {
            b.draw(this)
        }
    }
}