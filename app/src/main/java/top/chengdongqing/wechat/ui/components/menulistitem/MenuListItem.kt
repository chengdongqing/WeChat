package top.chengdongqing.wechat.ui.components.menulistitem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun MenuListItem(
    title: String,
    @DrawableRes iconResId: Int,
    iconColor: Color = Color.Unspecified,
    onTap: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(WeChatTheme.colorScheme.surface)
            .clickable { onTap?.invoke() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = WeChatTheme.colorScheme.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_right_outline),
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}