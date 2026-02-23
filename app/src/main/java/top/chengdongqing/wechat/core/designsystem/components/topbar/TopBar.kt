package top.chengdongqing.wechat.core.designsystem.components.topbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable

/**
 * 顶部导航栏
 */
@Composable
fun WeTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    containerColor: Color = WeTheme.colorScheme.background,
    contentColor: Color = WeTheme.colorScheme.textPrimary,
    onBack: (() -> Unit)? = null,
    @DrawableRes backIconResId: Int = R.drawable.ic_back_outlined,
    backText: String? = null,
    actions: @Composable WeTopBarScope.() -> Unit = {}
) {
    Surface(
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(50.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            // 返回按钮
            if (onBack != null) {
                if (backText == null) {
                    WeTopBarIcon(
                        modifier = Modifier.align(Alignment.CenterStart),
                        iconResId = backIconResId,
                        description = "返回",
                        tint = contentColor,
                        onClick = onBack
                    )
                } else {
                    Text(
                        text = backText,
                        color = contentColor,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 8.dp)
                            .weClickable(onClick = onBack)
                    )
                }
            }

            // 标题
            if (title != null) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 56.dp),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                titleContent?.invoke()
            }

            // 右侧操作按钮
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeTopBarScopeImpl(contentColor).actions()
            }
        }
    }
}

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
        @DrawableRes iconResId: Int,
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
private class WeTopBarScopeImpl(
    private val contentColor: Color
) : WeTopBarScope {

    @Composable
    override fun ActionIcon(
        iconResId: Int,
        modifier: Modifier,
        description: String?,
        onClick: (() -> Unit)?
    ) {
        WeTopBarIcon(
            iconResId = iconResId,
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
    @DrawableRes iconResId: Int,
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
            painter = painterResource(iconResId),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            tint = tint
        )
    }
}