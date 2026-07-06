package top.chengdongqing.wechat.core.designsystem.components.topbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.onTap

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
    unreadCount: Int = 0,
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
                        WeTopBarIcon(
                            icon = backIconResId,
                            description = "返回",
                            tint = contentColor,
                            onClick = onBack
                        )
                    }
                } else {
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