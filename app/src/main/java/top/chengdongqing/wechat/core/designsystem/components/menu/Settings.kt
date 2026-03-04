package top.chengdongqing.wechat.core.designsystem.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White

@Composable
fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        color = WeTheme.colorScheme.textSecondary,
        fontSize = 13.sp,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .padding(top = 10.dp)
    )
    Column(modifier = Modifier.background(White)) {
        content()
    }
}

@Composable
fun SettingItem(
    label: String,
    showDivider: Boolean = true,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    MenuListItem(
        label = label,
        trailing = trailing,
        height = 52.dp,
        showArrow = showArrow,
        onClick = onClick
    )

    if (showDivider) {
        WeDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
fun RowScope.SettingValue(text: String?) {
    text?.let {
        Text(
            text = text,
            fontSize = 16.sp,
            color = WeTheme.colorScheme.textSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}