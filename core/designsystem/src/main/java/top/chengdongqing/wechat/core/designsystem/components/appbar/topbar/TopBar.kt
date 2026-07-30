package top.chengdongqing.wechat.core.designsystem.components.appbar.topbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

/**
 * 顶部导航栏
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun WeTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    containerColor: Color = WeTheme.colorScheme.background,
    contentColor: Color = WeTheme.colorScheme.textPrimary,
    onBack: (() -> Unit)? = null,
    @DrawableRes backIconResId: Int = R.drawable.ic_back_outlined,
    unreadCount: Int = 0,
    backText: String? = null,
    actions: @Composable TopAppBarScope.() -> Unit = {}
) {
    Surface(
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 使用忽略可见性的稳定 inset。即使媒体页或系统手势临时隐藏状态栏，
                // 顶栏高度也不会塌缩，退出全屏时不会发生纵向跳动。
                .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                .height(50.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            // 返回按钮
            LeftPart(
                onBack = onBack,
                backIconResId = backIconResId,
                unreadCount = unreadCount,
                backText = backText,
                contentColor = contentColor
            )

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
                TopAppBarScopeImpl(contentColor).actions()
            }
        }
    }
}

@Composable
private fun BoxScope.LeftPart(
    onBack: (() -> Unit)?,
    @DrawableRes backIconResId: Int,
    unreadCount: Int,
    backText: String?,
    contentColor: Color
) {
    if (onBack == null) return

    when {
        backText == null -> {
            WeBadge(
                visible = unreadCount > 0,
                content = unreadCount.toBadgeText(),
                alignment = Alignment.CenterEnd,
                size = 20.dp,
                gap = (-8).dp,
                contentColor = WeTheme.colorScheme.textPrimary,
                containerColor = WeTheme.colorScheme.divider,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                IconButton(
                    icon = backIconResId,
                    description = "返回",
                    tint = contentColor,
                    onClick = onBack
                )
            }
        }

        else -> {
            Text(
                text = backText,
                color = contentColor,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 8.dp)
                    .onTap(onClick = onBack)
            )
        }
    }
}
