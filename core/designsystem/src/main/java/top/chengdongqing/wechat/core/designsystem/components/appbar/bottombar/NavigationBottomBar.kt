package top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

interface NavigationTab {
    @get:StringRes
    val labelRes: Int

    @get:DrawableRes
    val iconRes: Int

    @get:DrawableRes
    val selectedIconRes: Int
}

@Composable
fun <T : NavigationTab> WeNavigationBottomBar(
    tabs: List<T>,
    badgeMap: Map<T, Int>,
    currentTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
    ) {
        WeDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                TabItem(
                    tab = tab,
                    isSelected = currentTabIndex == index,
                    badge = badgeMap[tab] ?: 0,
                    onClick = { onTabSelected(index) })
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    tab: NavigationTab,
    isSelected: Boolean,
    badge: Int,
    onClick: () -> Unit
) {
    val currentIcon = if (isSelected) tab.selectedIconRes else tab.iconRes
    val currentColor = if (isSelected) {
        WeTheme.colorScheme.primary
    } else {
        WeTheme.colorScheme.textPrimary
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                indication = null,
                interactionSource = null,
                role = Role.Tab
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WeBadge(
            visible = badge > 0,
            content = badge.toBadgeText(),
            size = 20.dp,
            offset = DpOffset(x = 12.dp, y = (-2).dp)
        ) {
            Icon(
                painter = painterResource(currentIcon),
                contentDescription = stringResource(tab.labelRes),
                modifier = Modifier.size(26.dp),
                tint = currentColor
            )
        }

        Text(
            text = stringResource(tab.labelRes),
            fontSize = 12.sp,
            color = currentColor
        )
    }
}