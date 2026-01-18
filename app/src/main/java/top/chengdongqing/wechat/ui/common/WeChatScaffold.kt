package top.chengdongqing.wechat.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.ui.chatlist.ChatListScreen
import top.chengdongqing.wechat.ui.contacts.ContactScreen
import top.chengdongqing.wechat.ui.discovery.DiscoveryScreen
import top.chengdongqing.wechat.ui.me.MeScreen
import top.chengdongqing.wechat.ui.navigation.Screen
import top.chengdongqing.wechat.ui.navigation.bottomTabItems

@Composable
fun WeChatScaffold() {
    val pagerState = rememberPagerState(pageCount = { bottomTabItems.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            WeChatTopBar()
        },
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
