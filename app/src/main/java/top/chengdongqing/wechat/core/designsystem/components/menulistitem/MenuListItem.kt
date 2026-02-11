package top.chengdongqing.wechat.core.designsystem.components.menulistitem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun MenuListItem(
    label: String,
    @DrawableRes iconResId: Int? = null,
    iconColor: Color = Color.Unspecified,
    description: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    showArrow: Boolean = true,
    height: Dp = 56.dp,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(WeTheme.colorScheme.surface)
            .clickable(enabled = showArrow) { onClick?.invoke() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconResId?.let { MenuIcon(it, iconColor) }
            MenuLabel(label, description)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            trailing?.invoke(this)
            if (showArrow) {
                MenuArrow()
            }
        }
    }
}

@Composable
private fun MenuIcon(
    @DrawableRes iconResId: Int,
    iconColor: Color
) {
    Icon(
        painter = painterResource(iconResId),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = iconColor
    )
}

@Composable
private fun MenuLabel(label: String, description: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = WeTheme.colorScheme.textPrimary
        )
        if (!description.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = WeTheme.colorScheme.textSecondary
            )
        }
    }
}

@Composable
private fun MenuArrow() {
    Icon(
        painter = painterResource(R.drawable.ic_right_outlined),
        contentDescription = null,
        tint = Color.DarkGray,
        modifier = Modifier
            .size(24.dp)
            .offset(x = 8.dp)
    )
}