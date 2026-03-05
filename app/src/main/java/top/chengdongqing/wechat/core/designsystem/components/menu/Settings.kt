package top.chengdongqing.wechat.core.designsystem.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White

@Composable
fun WeSettingGroup(title: String? = null, content: @Composable () -> Unit) {
    Column {
        title?.let {
            Text(
                text = title,
                fontSize = 13.sp,
                color = WeTheme.colorScheme.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        Column(modifier = Modifier.background(White)) {
            content()
        }
    }
}

@Composable
fun WeSettingItem(
    label: String,
    description: String? = null,
    showDivider: Boolean = true,
    showArrow: Boolean = true,
    height: Dp = 52.dp,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    WeMenuListItem(
        label = label,
        description = description,
        trailing = trailing,
        height = height,
        showArrow = showArrow,
        onClick = onClick
    )

    if (showDivider) {
        WeDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
fun RowScope.WeSettingValue(text: String?) {
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

@Composable
fun WeDangerButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.error
        )
    }
}