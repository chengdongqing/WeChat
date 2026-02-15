package top.chengdongqing.wechat.features.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.components.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBarIcon
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.navigation.Screen
import top.chengdongqing.wechat.features.chat.navigation.ChatRoute
import top.chengdongqing.wechat.features.chat.ui.list.ChatListScreen
import top.chengdongqing.wechat.features.contacts.navigation.ContactsRoute
import top.chengdongqing.wechat.features.contacts.ui.list.ContactListScreen
import top.chengdongqing.wechat.features.discovery.DiscoveryScreen
import top.chengdongqing.wechat.features.home.navigation.HomeTab
import top.chengdongqing.wechat.features.home.ui.components.MenuItem
import top.chengdongqing.wechat.features.home.ui.components.QuickActions
import top.chengdongqing.wechat.features.me.ui.MeScreen
import top.chengdongqing.wechat.features.me.ui.profile.HandleProfileNavigationEvents
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { HomeTab.tabs.size }
    )
    val scope = rememberCoroutineScope()
    val currentTab = HomeTab.tabs[pagerState.currentPage]

    // 处理导航事件
    HandleProfileNavigationEvents(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onNavigateToContactDetail = { id ->
            navController.navigate(ContactsRoute.Detail.createRoute(id))
        },
        onNavigateToPlainText = { text ->
            navController.navigate(Screen.PlainText.createRoute(text))
        },
        onNavigateToWebView = { url ->
            navController.navigate(Screen.WebView.createRoute(url))
        }
    )

    Scaffold(
        topBar = {
            HomeTopBarWrapper(
                currentTab = currentTab,
                viewModel = viewModel,
                navController = navController
            )
        },
        bottomBar = {
            HomeBottomBar(
                pagerState = pagerState,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.scrollToPage(index)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        HomeContentPager(
            pagerState = pagerState,
            innerPadding = innerPadding,
            navController = navController
        )
    }

    LoadingDialog(uiState.isLoading)
}

/**
 * TopBar 包装器，根据当前 Tab 显示不同内容
 */
@Composable
private fun HomeTopBarWrapper(
    currentTab: HomeTab,
    viewModel: ProfileViewModel,
    navController: NavHostController
) {
    if (currentTab.route != HomeTab.Me.route) {
        HomeTopBar(
            title = currentTab.label,
            viewModel = viewModel,
            onNavigateToAddFriend = {
                navController.navigate(ContactsRoute.AddContact.route)
            }
        )
    } else {
        Surface(
            color = WeTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(50.dp)
            ) {}
        }
    }
}

/**
 * 首页内容分页器
 */
@Composable
private fun HomeContentPager(
    pagerState: PagerState,
    innerPadding: PaddingValues,
    navController: NavHostController
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        beyondViewportPageCount = 1 // 预加载相邻页面
    ) { page ->
        when (HomeTab.tabs[page]) {
            HomeTab.Chats -> ChatListScreen(
                onNavigateToDetail = { id ->
                    navController.navigate(ChatRoute.ChatSession.createRoute(id))
                }
            )

            HomeTab.Contacts -> ContactListScreen(
                onNavigateToDetail = { id ->
                    navController.navigate(ContactsRoute.Detail.createRoute(id))
                },
                onNavigateToNewFriends = {
                    navController.navigate(ContactsRoute.NewFriends.route)
                }
            )

            HomeTab.Discovery -> DiscoveryScreen()

            HomeTab.Me -> MeScreen(navController)
        }
    }
}

/**
 * 首页顶部栏
 */
@Composable
private fun HomeTopBar(
    title: String,
    viewModel: ProfileViewModel,
    onNavigateToAddFriend: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var anchorPosition by remember { mutableStateOf(Offset.Zero) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }

    val launchScanner = rememberScanCodeLauncher { qrCodes ->
        viewModel.handleScannedQRCode(qrCodes.first())
    }

    val menuItems = rememberHomeMenuItems(
        onNavigateToAddFriend = onNavigateToAddFriend,
        launchScanner = launchScanner
    )

    Column {
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
                anchorSize = anchorSize,
                onDismiss = { menuExpanded = false }
            )
        }

        WeDivider()
    }
}

@Composable
private fun HomeBottomBar(
    viewModel: HomeViewModel = hiltViewModel(),
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
) {
    val unreadCounts by viewModel.unreadCounts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.tabBarBackground)
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
                    WeTheme.colorScheme.tabBarIconInactive
                }
                val badge = unreadCounts[screen] ?: 0

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .weClickable { onTabSelected(index) },
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
                        text = screen.label,
                        fontSize = 12.sp,
                        color = currentColor
                    )
                }
            }
        }
    }
}

/**
 * 记忆化首页菜单项
 */
@Composable
private fun rememberHomeMenuItems(
    onNavigateToAddFriend: () -> Unit,
    launchScanner: () -> Unit
): List<MenuItem> {
    return remember(onNavigateToAddFriend, launchScanner) {
        listOf(
            MenuItem(R.drawable.ic_chats_filled, "发起群聊") { },
            MenuItem(R.drawable.ic_add_friends_filled, "添加朋友", onNavigateToAddFriend),
            MenuItem(R.drawable.ic_scan_filled, "扫一扫", launchScanner),
            MenuItem(R.drawable.ic_pay_vendor_filled, "收付款") { }
        )
    }
}