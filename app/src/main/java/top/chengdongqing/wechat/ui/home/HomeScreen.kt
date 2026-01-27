package top.chengdongqing.wechat.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.chat.list.ChatListScreen
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.components.badge.WeBadge
import top.chengdongqing.wechat.ui.components.badge.toBadgeText
import top.chengdongqing.wechat.ui.components.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.components.topbar.WeTopBarIcon
import top.chengdongqing.wechat.ui.contacts.ContactsScreen
import top.chengdongqing.wechat.ui.discovery.DiscoveryScreen
import top.chengdongqing.wechat.ui.me.MeScreen
import top.chengdongqing.wechat.ui.navigation.Screen
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.utils.weClickable

@Composable
fun HomeScreen(navController: NavHostController) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { bottomTabItems.size }
    )
    val scope = rememberCoroutineScope()
    val currentTab = bottomTabItems[pagerState.currentPage]

    Scaffold(
        topBar = {
            if (currentTab.route != "me")
                TopBar(currentTab.label, navController)
            else
                Surface(
                    color = WeChatTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(50.dp)
                    ) {}
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
                Screen.Chats -> ChatListScreen {
                    navController.navigate(Screen.ChatSession.createRoute(it))
                }

                Screen.Contacts -> ContactsScreen()
                Screen.Discovery -> DiscoveryScreen()
                Screen.Me -> MeScreen()
                else -> {}
            }
        }
    }
}

@Composable
private fun TopBar(title: String, navController: NavHostController) {
    var menuExpanded by remember { mutableStateOf(false) }
    var anchorPosition by remember { mutableStateOf(Offset.Zero) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    val scanCode = rememberScanCodeLauncher {}

    val menuItems = remember {
        listOf(
            MenuItem(R.drawable.ic_chats_filled, "发起群聊") { },
            MenuItem(R.drawable.ic_add_friends_filled, "添加朋友") {
                navController.navigate(Screen.AddFriend.route)
            },
            MenuItem(R.drawable.ic_scan_filled, "扫一扫") {
                scanCode()
            },
            MenuItem(R.drawable.ic_pay_vendor_filled, "收付款") { }
        )
    }

    WeTopBar(title = title) {
        ActionIcon(
            iconResId = R.drawable.ic_search_outlined,
            description = "搜索"
        )

        WeTopBarIcon(
            modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                anchorPosition = layoutCoordinates.positionInParent()
                anchorSize = layoutCoordinates.size
            },
            iconResId = R.drawable.ic_plus_circle_outlined,
            description = "更多"
        ) {
            menuExpanded = true
        }
        QuickActions(
            expanded = menuExpanded,
            items = menuItems,
            anchorPosition = anchorPosition,
            anchorSize = anchorSize
        ) {
            menuExpanded = false
        }
    }
}

@Composable
private fun BottomBar(
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
                    .height(56.dp),
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
                        WeBadge(
                            visible = index == 0,
                            content = 6.toBadgeText(),
                            size = 20.dp,
                            offset = DpOffset(x = 12.dp, y = (-2).dp)
                        ) {
                            Icon(
                                painter = painterResource(currentIconResId),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = currentColor
                            )
                        }
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

private val bottomTabItems = listOf(
    Screen.Chats,
    Screen.Contacts,
    Screen.Discovery,
    Screen.Me
)