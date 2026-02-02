package top.chengdongqing.wechat.ui.components.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.theme.WeTheme

/**
 * 微信风格的通用输入框组件
 *
 * @param value 当前输入的内容
 * @param modifier 外部修饰符
 * @param label 输入框左侧的标签文本，为 null 时不显示
 * @param placeholder 当输入为空时的提示文本
 * @param maxLength 最大字符长度限制，为 null 时不限制
 * @param singleLine 是否强制单行显示，默认为 true
 * @param maxLines 最大显示行数
 * @param activeColor 获取焦点时底部横线的颜色，默认为微信绿
 * @param inactiveColor 未获取焦点时底部横线的颜色，默认为浅灰色
 * @param onValueChange 内容改变时的回调
 */
@Composable
fun WeInput(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    maxLength: Int? = null,
    singleLine: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    activeColor: Color = Color(0xFF07C160),
    inactiveColor: Color = Color(0xFFE5E5E5),
    onValueChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val indicatorColor by animateColorAsState(
        targetValue = if (isFocused) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 300),
        label = "IndicatorColorAnimation"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧标签
            label?.let {
                Text(
                    text = it,
                    fontSize = 17.sp,
                    color = WeTheme.colorScheme.textPrimary
                )
                Spacer(modifier = Modifier.width(24.dp))
            }

            // 核心输入区域
            BasicTextField(
                value = value,
                onValueChange = {
                    if (maxLength == null || it.length <= maxLength) {
                        onValueChange(it)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    color = WeTheme.colorScheme.textPrimary
                ),
                singleLine = singleLine,
                maxLines = maxLines,
                cursorBrush = SolidColor(WeTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color.LightGray,
                                fontSize = 17.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // 字数剩余提示 (仅在设置了 maxLength 时显示)
            maxLength?.let { max ->
                Text(
                    text = "${max - value.length}",
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        // 底部动态横线
        WeDivider(
            thickness = 1.dp,
            color = indicatorColor
        )
    }
}