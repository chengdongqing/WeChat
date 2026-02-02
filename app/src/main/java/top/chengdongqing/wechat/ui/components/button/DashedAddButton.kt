package top.chengdongqing.wechat.ui.components.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.util.dashedBorder

/**
 * 通用的虚线边框新增按钮
 *
 * @param modifier 外部修饰符
 * @param color 边框和图标的颜色 (默认为 Black)
 * @param strokeWidth 虚线边框的粗细 (默认为 1.dp)
 * @param cornerRadius 圆角大小 (默认为 10.dp)
 * @param iconSize 图标的大小 (默认为 30.dp)
 * @param onClick 点击回调
 */
@Composable
fun DashedAddButton(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    strokeWidth: Dp = 1.dp,
    cornerRadius: Dp = 10.dp,
    iconSize: Dp = 30.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .dashedBorder(
                width = strokeWidth,
                color = color,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_plus_outlined),
            contentDescription = "Add Item",
            modifier = Modifier.size(iconSize),
            tint = color
        )
    }
}