package top.chengdongqing.wechat.core.designsystem.components.emojitextfield

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.withTranslation
import top.chengdongqing.wechat.core.designsystem.model.Emojis
import top.chengdongqing.wechat.core.designsystem.theme.BrandPrimary
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.EmojiRenderer

/**
 * 配置常量
 */
private object EmojiTextFieldConfig {
    /** 默认字体大小（sp） */
    const val DEFAULT_FONT_SIZE_SP = 16

    /** 默认最大高度 */
    val DEFAULT_MAX_HEIGHT_DP = 120.dp

    /** 表情图片相对字体大小的缩放比例 */
    const val EMOJI_SIZE_MULTIPLIER = 1.3f

    /** 光标宽度（dp） */
    const val CURSOR_WIDTH_DP = 1.8f
}

@Composable
fun EmojiTextField(
    value: String,
    modifier: Modifier = Modifier,
    focusRequester: NativeFocusRequester,
    fontSizeSp: Int = EmojiTextFieldConfig.DEFAULT_FONT_SIZE_SP,
    textColor: androidx.compose.ui.graphics.Color = WeTheme.colorScheme.textPrimary,
    maxHeightDp: Dp? = EmojiTextFieldConfig.DEFAULT_MAX_HEIGHT_DP,
    onValueChange: (String) -> Unit,
    onLineCountChange: ((Int) -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // 预计算像素值
    val cursorWidthPx = with(density) { EmojiTextFieldConfig.CURSOR_WIDTH_DP.dp.toPx().toInt() }
    val fontSizePx = with(density) { fontSizeSp.sp.toPx().toInt() }
    val maxHeightPx = with(density) { maxHeightDp?.toPx()?.toInt() }

    // 缓存上次处理的文本，避免重复解析
    val lastProcessedText = remember { mutableStateOf("") }
    // 避免闭包捕获旧的 lambda
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnLineCountChange by rememberUpdatedState(onLineCountChange)
    val currentOnImeAction by rememberUpdatedState(onImeAction)

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        factory = { ctx ->
            AppCompatEditText(ctx).apply {
                setupConfig(fontSizeSp, cursorWidthPx, maxHeightPx)
                focusRequester.bind(this)

                if (imeAction != ImeAction.Default) {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                    val resolvedImeOptions = when (imeAction) {
                        ImeAction.Send -> EditorInfo.IME_ACTION_SEND
                        ImeAction.Done -> EditorInfo.IME_ACTION_DONE
                        ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
                        else -> EditorInfo.IME_ACTION_UNSPECIFIED
                    }
                    imeOptions = resolvedImeOptions

                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == resolvedImeOptions) {
                            currentOnImeAction?.invoke()
                            true
                        } else {
                            false
                        }
                    }
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
                        // 忽略由 update 回调引起的文本变更同步
                        if (tag != TAG_IGNORE_UPDATE) {
                            val newText = s?.toString() ?: ""

                            // 只有纯文本真正改变时才触发回调
                            if (newText != lastProcessedText.value) {
                                lastProcessedText.value = newText
                                currentOnValueChange(newText)

                                // 只在包含表情时才更新 Spannable
                                if (containsEmoji(newText)) {
                                    updateTextWithEmoji(this@apply, ctx, newText, fontSizePx)
                                }

                                // 更新行数
                                post { currentOnLineCountChange?.invoke(lineCount) }
                            }
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        },
        update = { editText ->
            editText.setTextColor(textColor.toArgb())

            // 只有当参数变化时才触发布局更新
            if (editText.maxHeight != (maxHeightPx ?: Int.MAX_VALUE)) {
                editText.maxHeight = maxHeightPx ?: Int.MAX_VALUE
            }

            // 避免重复更新：只在外部 value 真正改变时才更新
            if (editText.text.toString() != value && value != lastProcessedText.value) {
                lastProcessedText.value = value
                updateTextWithEmoji(editText, context, value, fontSizePx)
                editText.post { currentOnLineCountChange?.invoke(editText.lineCount) }
            }

            focusRequester.bind(editText)
        }
    )
}

/**
 * 快速检查文本是否包含表情标记
 * 避免不必要的正则匹配和 Spannable 创建
 */
private fun containsEmoji(text: String): Boolean {
    return text.contains('[') && text.contains(']')
}

private const val TAG_IGNORE_UPDATE = "IGNORE_UPDATE"

/**
 * EditText 初始化配置
 *
 * @param fontSizeSp 字体大小（sp）
 * @param cursorWidth 光标宽度（px）
 * @param maxH 最大高度（px），null 表示不限制
 */
private fun AppCompatEditText.setupConfig(fontSizeSp: Int, cursorWidth: Int, maxH: Int?) {
    background = null
    textSize = fontSizeSp.toFloat()
    gravity = Gravity.TOP or Gravity.START
    includeFontPadding = false
    isFocusableInTouchMode = true
    setPadding(0, 0, 0, 0)

    // Android 10+ 使用自定义光标
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        textCursorDrawable = GradientDrawable().apply {
            setColor(BrandPrimary.toArgb())
            setSize(cursorWidth, 0)
        }
    }

    maxH?.let { maxHeight = it }
}

/**
 * 更新文本并渲染表情图片，同时保持光标位置
 *
 * @param editText 目标 EditText
 * @param context Android Context
 * @param value 新的文本内容
 * @param sizePx 字体大小（px），用于计算表情图片大小
 */
private fun updateTextWithEmoji(
    editText: AppCompatEditText,
    context: Context,
    value: String,
    sizePx: Int
) {
    // 保存当前光标位置和文本长度
    val oldStart = editText.selectionStart
    val oldLength = editText.text?.length ?: 0

    // 标记为内部更新，避免触发 TextWatcher
    editText.tag = TAG_IGNORE_UPDATE

    // 解析表情并创建 Spannable
    val spannable = context.parseEmojiSpannable(value, sizePx)
    editText.setText(spannable)

    // 智能计算新的光标位置
    val newLength = spannable.length
    val newSelection = when {
        // 场景1：文本追加且光标在末尾 -> 光标跟随到新末尾
        newLength > oldLength && oldStart >= oldLength -> newLength

        // 场景2：文本删除 -> 光标相对后退
        newLength < oldLength -> (oldStart - (oldLength - newLength)).coerceIn(0, newLength)

        // 场景3：其他情况（替换、修改）-> 保持原位置
        else -> oldStart.coerceIn(0, newLength)
    }

    editText.setSelection(newSelection)

    // 清除标记，允许后续的 TextWatcher 触发
    editText.tag = null
}

/**
 * 解析文本中的表情标记并替换为图片 Span
 *
 * @param text 原始文本
 * @param fontSizePx 字体大小（px）
 * @return 包含表情图片的 Spannable
 */
private fun Context.parseEmojiSpannable(text: CharSequence, fontSizePx: Int): Spannable {
    val spannable = SpannableStringBuilder(text)
    val targetSize = (fontSizePx * EmojiTextFieldConfig.EMOJI_SIZE_MULTIPLIER).toInt()

    // 从后往前遍历，避免替换时索引偏移
    Emojis.findAllMatches(text).reversed().forEach { match ->
        val bitmap = EmojiRenderer.getBitmap(this, match.emoji, targetSize)
        val drawable = bitmap.toDrawable(resources).apply {
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

/**
 * 垂直居中的 ImageSpan
 *
 * 解决 ImageSpan 默认对齐 Baseline 或 Bottom 导致的视觉偏移问题。
 * 确保表情图片在文字行内垂直居中显示。
 */
private class VerticalCenterImageSpan(drawable: Drawable) : ImageSpan(drawable) {

    companion object {
        // 防止图片底部被裁切的缓冲像素
        private const val VERTICAL_BUFFER = 2
    }

    /**
     * 计算 Span 占用的宽度，并调整行高以容纳图片
     *
     * 当图片高度超过文字行高时，会上下对称地扩展 FontMetrics，
     * 确保图片完整显示且不会与上下行重叠。
     */
    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val rect = drawable.bounds

        if (fm != null) {
            val fontMetrics = paint.fontMetricsInt
            val fontHeight = fontMetrics.bottom - fontMetrics.top
            val drawableHeight = rect.height()

            // 只有当图片高于文字时才需要扩展行高
            if (drawableHeight > fontHeight) {
                val offset = (drawableHeight - fontHeight) / 2

                // 向上扩展（负值更小）
                fm.ascent = fontMetrics.ascent - offset
                fm.top = fontMetrics.top - offset

                // 向下扩展（正值更大），并添加缓冲防止裁切
                fm.descent = fontMetrics.descent + offset + VERTICAL_BUFFER
                fm.bottom = fontMetrics.bottom + offset + VERTICAL_BUFFER
            }
        }

        return rect.right
    }

    /**
     * 在 Canvas 上绘制图片
     *
     * 计算垂直居中位置：文本中心点 - 图片高度的一半
     */
    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val drawable = drawable
        val fm = paint.fontMetricsInt

        // 计算文本中心 Y 坐标（Baseline + (Descent + Ascent) / 2）
        val textCenterY = (y + fm.descent + y + fm.ascent) / 2f

        // 计算图片中心到顶部的距离
        val drawableCenterOffset = drawable.bounds.height() / 2f

        // 最终的 Y 坐标 = 文本中心 - 图片半高
        val transY = textCenterY - drawableCenterOffset

        // 平移并绘制
        canvas.withTranslation(x, transY) {
            drawable.draw(this)
        }
    }
}