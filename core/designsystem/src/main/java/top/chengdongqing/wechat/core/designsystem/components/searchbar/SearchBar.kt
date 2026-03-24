package top.chengdongqing.wechat.core.designsystem.components.searchbar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.LinkColor
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable

@Composable
fun WeSearchBar(
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.action_search),
    disabled: Boolean = false,
    backgroundColor: Color = WeTheme.colorScheme.background,
    focused: Boolean? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onChange: (value: String) -> Unit
) {
    var localFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val isFocused = focused ?: localFocused
    fun setFocus(value: Boolean) {
        localFocused = value
        onFocusChange?.invoke(value)
    }

    // 输入框自动聚焦
    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }
    // 返回时先取消聚焦
    BackHandler(isFocused) {
        setFocus(false)
    }

    Row(
        modifier = modifier.height(38.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(backgroundColor)
        ) {
            if (isFocused) {
                BasicTextField(
                    value,
                    onValueChange = onChange,
                    modifier = Modifier.focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = WeTheme.colorScheme.textPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    cursorBrush = SolidColor(WeTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        SearchBarDecoration(value, placeholder, innerTextField)
                    })
            } else {
                SearchPlaceholder(placeholder) {
                    if (!disabled) {
                        setFocus(true)
                    }
                    onClick?.invoke()
                }
            }
        }
        if (isFocused) {
            Text(
                text = stringResource(R.string.action_cancel),
                color = LinkColor,
                fontSize = 16.sp,
                modifier = Modifier
                    .weClickable {
                        setFocus(false)
                        onChange("")
                    }
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchBarDecoration(
    value: String,
    placeholder: String,
    innerTextField: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp, end = 4.dp)
                .size(20.dp),
            tint = WeTheme.colorScheme.textSecondary
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 16.sp
                )
            }
            innerTextField()
        }
    }
}

@Composable
private fun SearchPlaceholder(
    placeholder: String,
    onActivate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .weClickable(onClick = onActivate),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search_outlined),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(20.dp),
            tint = WeTheme.colorScheme.textSecondary
        )
        Text(
            text = placeholder,
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 16.sp
        )
    }
}