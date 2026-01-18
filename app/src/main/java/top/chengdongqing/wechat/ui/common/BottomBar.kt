package top.chengdongqing.wechat.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.util.weClickable
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.navigation.bottomTabItems
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun BottomBar(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = WeChatTheme.colorScheme.tabBarBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            WeDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // 自动处理系统底栏高度
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomTabItems.forEachIndexed { index, screen ->
                    val isSelected = pagerState.currentPage == index

                    val currentIconResId =
                        if (isSelected) screen.selectedIconResId!! else screen.iconResId!!
                    val currentColor =
                        if (isSelected) WeChatTheme.colorScheme.primary else WeChatTheme.colorScheme.tabBarIconInactive

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .weClickable { onTabSelected(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(currentIconResId),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = currentColor
                        )
                        Text(
                            text = screen.label,
                            fontSize = 12.sp,
                            color = currentColor
                        )
                    }
                }
            }
        }
    }
}