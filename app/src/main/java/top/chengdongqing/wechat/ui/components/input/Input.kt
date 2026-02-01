package top.chengdongqing.wechat.ui.components.input

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun WeInput(
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLength: Int? = null,
    singleLine: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onValueChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 输入框
            BasicTextField(
                value = value,
                onValueChange = {
                    if (maxLength == null || it.length <= maxLength) {
                        onValueChange(it)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(fontSize = 17.sp, color = Color.Black),
                singleLine = singleLine,
                maxLines = maxLines,
                cursorBrush = SolidColor(WeChatTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, color = Color.LightGray, fontSize = 17.sp)
                        }
                        innerTextField()
                    }
                }
            )

            // 字数提示
            maxLength?.let { maxLength ->
                Box(modifier = Modifier.padding(start = 30.dp)) {
                    Text(
                        text = "${maxLength - value.length}",
                        color = WeChatTheme.colorScheme.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        // 底部线
        WeDivider(
            thickness = 1.dp,
            color = if (isFocused) Color(0xFF07C160) else Color(0xFFE5E5E5)
        )
    }
}