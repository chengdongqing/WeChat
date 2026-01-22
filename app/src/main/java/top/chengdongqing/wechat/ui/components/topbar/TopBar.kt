package top.chengdongqing.wechat.ui.components.topbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.weClickable
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun WeTopBar(
    title: String,
    bgColor: Color = WeChatTheme.colorScheme.background,
    textColor: Color = WeChatTheme.colorScheme.textPrimary,
    onBack: (() -> Unit)? = null,
    actions: @Composable WeTopBarScope.() -> Unit = {}
) {
    Surface(
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding() // 自动处理系统顶栏高度
                .height(50.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (onBack != null) {
                WeTopBarIcon(
                    modifier = Modifier.align(Alignment.CenterStart),
                    iconResId = R.drawable.ic_back_outlined,
                    description = "返回",
                    tint = textColor,
                    onClick = onBack
                )
            }
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            val scope = remember(textColor) { WeTopBarScopeImpl(textColor) }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                scope.actions()
            }
        }
    }
}

@LayoutScopeMarker
interface WeTopBarScope {
    @Composable
    fun ActionIcon(
        modifier: Modifier = Modifier,
        @DrawableRes iconResId: Int,
        description: String? = null,
        tint: Color = WeChatTheme.colorScheme.textPrimary,
        onClick: (() -> Unit)? = null
    )
}

private class WeTopBarScopeImpl(private val textColor: Color) : WeTopBarScope {
    @Composable
    override fun ActionIcon(
        modifier: Modifier,
        iconResId: Int,
        description: String?,
        tint: Color,
        onClick: (() -> Unit)?
    ) {
        WeTopBarIcon(
            modifier = Modifier,
            iconResId = iconResId,
            description = description,
            tint = textColor,
            onClick = onClick,
        )
    }
}

@Composable
fun WeTopBarIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int,
    description: String? = null,
    tint: Color = WeChatTheme.colorScheme.textPrimary,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .weClickable(onClick = { onClick?.invoke() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            tint = tint
        )
    }
}