package top.chengdongqing.wechat.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.chatlist.ChatListScreen
import top.chengdongqing.wechat.ui.contacts.ContactScreen
import top.chengdongqing.wechat.ui.discovery.DiscoveryScreen
import top.chengdongqing.wechat.ui.me.MeScreen
import top.chengdongqing.wechat.ui.navigation.Screen
import top.chengdongqing.wechat.ui.navigation.bottomTabItems

@Composable
fun WeScaffold() {
    val pagerState = rememberPagerState(pageCount = { bottomTabItems.size })
    val scope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }

    val menuItems = listOf(
        MenuItem(R.drawable.ic_chats_filled, "发起群聊") { /* 逻辑 */ },
        MenuItem(R.drawable.ic_add_friends_filled, "添加朋友") { /* 逻辑 */ },
        MenuItem(R.drawable.ic_scan_filled, "扫一扫") { /* 逻辑 */ },
        MenuItem(R.drawable.ic_pay_vendor_filled, "收付款") { /* 逻辑 */ }
    )

    Scaffold(
        topBar = {
            TopBar(title = "微信") {
                TopBarIconButton(
                    iconResId = R.drawable.ic_search_outline,
                    description = "搜索"
                )

                Box {
                    TopBarIconButton(
                        iconResId = R.drawable.ic_plus_circle_outline,
                        description = "更多"
                    ) {
                        menuExpanded = true
                    }

                    DropDownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        items = menuItems
                    )
                }
            }
        },
        bottomBar = {
            BottomBar(
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
            modifier = Modifier.padding(innerPadding)
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
