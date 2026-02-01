package top.chengdongqing.wechat.ui.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.util.weClickable

/**
 * 通话顶部栏
 *
 * @param statusText 状态文本（如通话时长）
 * @param onMinimizeClick 最小化点击事件
 * @param isDarkBackground 是否为深色背景（影响渐变透明度）
 */
@Composable
fun CallTopBar(
    statusText: String,
    onMinimizeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkBackground: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = if (isDarkBackground) 0.6f else 0.4f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // 最小化按钮
        MinimizeButton(
            onClick = onMinimizeClick,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // 状态文本（居中显示）
        if (statusText.isNotEmpty()) {
            StatusText(
                text = statusText,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * 最小化按钮
 */
@Composable
private fun MinimizeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(R.drawable.ic_minimize_outlined),
        contentDescription = "最小化",
        modifier = modifier
            .size(32.dp)
            .offset(x = 14.dp)
            .weClickable(onClick = onClick),
        tint = Color.White
    )
}

/**
 * 状态文本
 */
@Composable
private fun StatusText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color.White.copy(alpha = 0.7f),
        modifier = modifier
    )
}