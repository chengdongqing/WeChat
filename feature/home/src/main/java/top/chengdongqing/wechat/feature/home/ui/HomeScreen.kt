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
import top.chengdongqing.wechat.core.common.navigation.NavigationKey
import top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar.WeNavigationBottomBar
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.chat.ui.list.ChatListScreen
import top.chengdongqing.wechat.feature.contacts.ui.list.ContactListScreen
import top.chengdongqing.wechat.feature.discovery.DiscoveryScreen
import top.chengdongqing.wechat.feature.home.model.HomeTab
import top.chengdongqing.wechat.feature.home.ui.components.HomeTopBar
import top.chengdongqing.wechat.feature.profile.ui.MeScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.HandleProfileNavigationEvents
import top.chengdongqing.wechat.feature.profile.ui.profile.ProfileViewModel

@Composable
fun HomeScreen(
    backStack: NavBackStack<NavKey>,
    homeViewModel: HomeViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val unreadMap by homeViewModel.unreadMap.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { HomeTab.entries.size }
    )
    val scope = rememberCoroutineScope()
    val currentTab = HomeTab.entries[pagerState.currentPage]

    // 处理导航事件
    HandleProfileNavigationEvents(
        viewModel = profileViewModel,
        snackbarHostState = snackbarHostState,
        onNavigateToContactDetail = { id ->
            backStack.add(NavigationKey.ContactDetail(id))
        },
        onNavigateToPlainText = { text ->
            backStack.add(NavigationKey.PlainText(text))
        },
        onNavigateToWebView = { url ->
            backStack.add(NavigationKey.WebView(url))
        }
    )

    Scaffold(
        topBar = {
            HomeTopBar(
                currentTab = currentTab,
                unreadMap = unreadMap,
                viewModel = profileViewModel,
                backStack = backStack
            )
        },
        bottomBar = {
            WeNavigationBottomBar(
                tabs = HomeTab.entries,
                currentTabIndex = pagerState.currentPage,
                badgeMap = unreadMap,
                onTabSelected = { index ->
                    if (index != pagerState.currentPage) {
                        scope.launch {
                            pagerState.scrollToPage(index)
                        }
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

    ProfileLoadingOverlay(profileViewModel)
}

@Composable
private fun ProfileLoadingOverlay(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        when (HomeTab.entries[page]) {
            HomeTab.Chats -> ChatListScreen(
                onNavigateToDetail = { backStack.add(NavigationKey.ChatSession(it)) }
            )

            HomeTab.Contacts -> ContactListScreen(
                onNavigateToNewFriends = { backStack.add(NavigationKey.NewFriends) },
                onNavigateToDetail = { backStack.add(NavigationKey.ContactDetail(it)) },
                onNavigateToProfileEdit = { backStack.add(NavigationKey.EditContactProfile(it)) }
            )

            HomeTab.Discovery -> DiscoveryScreen()

            HomeTab.Me -> MeScreen(backStack)
        }
    }
}