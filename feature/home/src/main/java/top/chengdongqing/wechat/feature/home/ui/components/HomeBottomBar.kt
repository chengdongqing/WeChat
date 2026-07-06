package top.chengdongqing.wechat.feature.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.onTap
import top.chengdongqing.wechat.feature.home.model.HomeTab
import top.chengdongqing.wechat.feature.home.theme.HomeTheme

@Composable
fun HomeBottomBar(
    unreadCounts: Map<HomeTab, Int>,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HomeTheme.colorScheme.tabBarBackground)
    ) {
        WeDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.tabs.forEachIndexed { index, screen ->
                val isSelected = pagerState.currentPage == index
                val currentIcon = if (isSelected) screen.selectedIcon else screen.icon
                val currentColor = if (isSelected) {
                    WeTheme.colorScheme.primary
                } else {
                    HomeTheme.colorScheme.tabBarIconInactive
                }
                val badge = unreadCounts[screen] ?: 0

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .onTap { onTabSelected(index) },
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
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = currentColor
                        )
                    }
                    Text(
                        text = stringResource(screen.label),
                        fontSize = 12.sp,
                        color = currentColor
                    )
                }
            }
        }
    }
}