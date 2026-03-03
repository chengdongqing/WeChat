package top.chengdongqing.wechat.core.designsystem.components.topbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable

/**
 * TopBar 操作按钮作用域
 */
@DslMarker
annotation class TopBarScopeMarker

@TopBarScopeMarker
interface WeTopBarScope {
    /**
     * 操作按钮（图标）
     */
    @Composable
    fun ActionIcon(
        @DrawableRes icon: Int,
        modifier: Modifier = Modifier,
        description: String? = null,
        onClick: (() -> Unit)? = null
    )

    /**
     * 操作按钮（文字）
     */
    @Composable
    fun ActionText(
        text: String,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null
    )
}

/**
 * Scope 实现（内部类，外部不可见）
 */
internal class WeTopBarScopeImpl(
    private val contentColor: Color
) : WeTopBarScope {

    @Composable
    override fun ActionIcon(
        icon: Int,
        modifier: Modifier,
        description: String?,
        onClick: (() -> Unit)?
    ) {
        WeTopBarIcon(
            icon = icon,
            description = description,
            tint = contentColor,
            modifier = modifier,
            onClick = onClick
        )
    }

    @Composable
    override fun ActionText(
        text: String,
        modifier: Modifier,
        onClick: (() -> Unit)?
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 16.sp,
            modifier = modifier
                .padding(horizontal = 8.dp)
                .weClickable(onClick = onClick ?: {})
        )
    }
}

/**
 * TopBar 图标按钮（可独立使用）
 */
@Composable
fun WeTopBarIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    tint: Color = WeTheme.colorScheme.textPrimary,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .weClickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            tint = tint
        )
    }
}