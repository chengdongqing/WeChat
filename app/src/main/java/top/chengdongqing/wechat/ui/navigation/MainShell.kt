package top.chengdongqing.wechat.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.ui.chatlist.ChatListScreen
import top.chengdongqing.wechat.ui.contacts.ContactScreen
import top.chengdongqing.wechat.ui.discovery.DiscoveryScreen
import top.chengdongqing.wechat.ui.me.MeScreen

@Composable
fun MainShell() {
    val pagerState = rememberPagerState(pageCount = { bottomTabItems.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            WeChatBottomBar(
                pagerState = pagerState,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.scrollToPage(index)
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
            // 禁止在非主页滑动
            userScrollEnabled = true
        ) { page ->
            when (bottomTabItems[page]) {
                Screen.Chats -> ChatListScreen()
                Screen.Contacts -> ContactScreen()
                Screen.Discovery -> DiscoveryScreen()
                Screen.Me -> MeScreen()
                else -> {}
            }
        }
    }
}

@Composable
fun WeChatBottomBar(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(containerColor = Color.White) {
        bottomTabItems.forEachIndexed { index, screen ->
            val isSelected = pagerState.currentPage == index

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                label = { Text(screen.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF07C160),
                    selectedTextColor = Color(0xFF07C160),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}