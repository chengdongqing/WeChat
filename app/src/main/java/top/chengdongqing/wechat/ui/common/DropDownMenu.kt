package top.chengdongqing.wechat.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MenuItem(
    @get:DrawableRes val icon: Int,
    val text: String,
    val onClick: () -> Unit
)

@Composable
fun DropDownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MenuItem>
) {
    // 使用 Material 提供的 DropdownMenu，它内置了位置计算和基础动画
    MaterialTheme(
        // 这里可以局部修改暗色主题，因为微信的弹出菜单通常是深色的
        colorScheme = darkColorScheme(surface = Color(0xFF4C4C4C))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .width(160.dp)
                .background(Color(0xFF4C4C4C), RoundedCornerShape(4.dp))
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.text, color = Color.White, fontSize = 16.sp)
                    },
                    leadingIcon = {
                        Icon(
                            painterResource(item.icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        item.onClick()
                        onDismissRequest() // 点击后自动关闭
                    }
                )
            }
        }
    }
}