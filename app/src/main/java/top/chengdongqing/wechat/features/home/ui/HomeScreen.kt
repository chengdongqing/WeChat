package top.chengdongqing.wechat.features.home.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.Screen
import top.chengdongqing.wechat.features.chat.navigation.ChatRoute
import top.chengdongqing.wechat.features.chat.ui.list.ChatListScreen
import top.chengdongqing.wechat.features.contacts.navigation.ContactsRoute
import top.chengdongqing.wechat.features.contacts.ui.list.ContactListScreen
import top.chengdongqing.wechat.features.discovery.DiscoveryScreen
import top.chengdongqing.wechat.features.home.model.HomeTab
import top.chengdongqing.wechat.features.home.ui.components.HomeBottomBar
import top.chengdongqing.wechat.features.home.ui.components.HomeTopBarWrapper
import top.chengdongqing.wechat.features.profile.ui.MeScreen
import top.chengdongqing.wechat.features.profile.ui.profile.HandleProfileNavigationEvents
import top.chengdongqing.wechat.features.profile.ui.profile.ProfileViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val unreadCounts by viewModel.unreadCounts.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { HomeTab.tabs.size }
    )
    val scope = rememberCoroutineScope()
    val currentTab = HomeTab.tabs[pagerState.currentPage]

    // 处理导航事件
    HandleProfileNavigationEvents(
        viewModel = profileViewModel,
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
                unreadCounts = unreadCounts,
                viewModel = profileViewModel,
                navController = navController
            )
        },
        bottomBar = {
            HomeBottomBar(
                unreadCounts = unreadCounts,
                pagerState = pagerState,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.scrollToPage(index)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        HomeContentPager(
            pagerState = pagerState,
            innerPadding = innerPadding,
            navController = navController
        )
    }

    LoadingDialog(uiState.isLoading)
}

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
                onNavigateToNewFriends = {
                    navController.navigate(ContactsRoute.NewFriends.route)
                },
                onNavigateToDetail = { id ->
                    navController.navigate(ContactsRoute.Detail.createRoute(id))
                },
                onNavigateToProfileEdit = { id ->
                    navController.navigate(ContactsRoute.ProfileEdit.createRoute(id))
                }
            )

            HomeTab.Discovery -> DiscoveryScreen()

            HomeTab.Me -> MeScreen(navController)
        }
    }
}