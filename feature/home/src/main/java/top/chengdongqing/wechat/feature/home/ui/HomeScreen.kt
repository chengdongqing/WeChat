package top.chengdongqing.wechat.feature.home.ui

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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.navigation.ChatKey
import top.chengdongqing.wechat.core.common.navigation.CommonKey
import top.chengdongqing.wechat.core.common.navigation.ContactsKey
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.chat.ui.list.ChatListScreen
import top.chengdongqing.wechat.feature.contacts.ui.list.ContactListScreen
import top.chengdongqing.wechat.feature.discovery.DiscoveryScreen
import top.chengdongqing.wechat.feature.home.model.HomeTab
import top.chengdongqing.wechat.feature.home.ui.components.HomeBottomBar
import top.chengdongqing.wechat.feature.home.ui.components.HomeTopBarWrapper
import top.chengdongqing.wechat.feature.profile.ui.MeScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.HandleProfileNavigationEvents
import top.chengdongqing.wechat.feature.profile.ui.profile.ProfileViewModel

@Composable
fun HomeScreen(
    backStack: NavBackStack<NavKey>,
    homeViewModel: HomeViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val unreadCounts by homeViewModel.unreadCounts.collectAsStateWithLifecycle()

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
            backStack.add(ContactsKey.Detail(id))
        },
        onNavigateToPlainText = { text ->
            backStack.add(CommonKey.PlainText(text))
        },
        onNavigateToWebView = { url ->
            backStack.add(CommonKey.WebView(url))
        }
    )

    Scaffold(
        topBar = {
            HomeTopBarWrapper(
                currentTab = currentTab,
                unreadCounts = unreadCounts,
                viewModel = profileViewModel,
                backStack = backStack
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
            backStack = backStack
        )
    }

    LoadingDialog(uiState.isLoading)
}

@Composable
private fun HomeContentPager(
    pagerState: PagerState,
    innerPadding: PaddingValues,
    backStack: NavBackStack<NavKey>
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
                onNavigateToDetail = { backStack.add(ChatKey.ChatSession(it)) }
            )

            HomeTab.Contacts -> ContactListScreen(
                onNavigateToNewFriends = { backStack.add(ContactsKey.NewFriends) },
                onNavigateToDetail = { backStack.add(ContactsKey.Detail(it)) },
                onNavigateToProfileEdit = { backStack.add(ContactsKey.EditProfile(it)) }
            )

            HomeTab.Discovery -> DiscoveryScreen()

            HomeTab.Me -> MeScreen(backStack)
        }
    }
}