package top.chengdongqing.wechat.app.shell

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar.WeNavigationBottomBar
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.chat.ui.list.ChatListScreen
import top.chengdongqing.wechat.feature.contacts.ui.list.ContactListScreen
import top.chengdongqing.wechat.feature.discovery.DiscoveryScreen
import top.chengdongqing.wechat.feature.profile.ui.MeScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.HandleProfileNavigationEvents
import top.chengdongqing.wechat.feature.profile.ui.profile.ProfileViewModel

@Composable
fun MainShellDestination(
    backStack: NavBackStack<NavKey>,
    mainShellViewModel: MainShellViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val unreadMap by mainShellViewModel.unreadMap.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { MainTab.entries.size })
    val selectedTabPosition by remember {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }
    val scope = rememberCoroutineScope()
    val currentTab = MainTab.entries[pagerState.currentPage]

    HandleProfileNavigationEvents(
        viewModel = profileViewModel,
        snackbarHostState = snackbarHostState,
        onContactDetail = { backStack.add(NavigationKey.ContactDetail(it)) },
        onPlainText = { backStack.add(NavigationKey.PlainText(it)) },
        onWebView = { backStack.add(NavigationKey.WebView(it)) }
    )

    Scaffold(
        topBar = {
            MainTopBar(
                currentTab = currentTab,
                unreadMap = unreadMap,
                onGroupChat = { backStack.add(NavigationKey.GroupChat("")) },
                onAddFriend = { backStack.add(NavigationKey.AddFriend) },
                onPayment = { backStack.add(NavigationKey.PaymentCode) },
                onScannedQrCode = profileViewModel::handleScannedQRCode
            )
        },
        bottomBar = {
            WeNavigationBottomBar(
                tabs = MainTab.entries,
                currentTabIndex = pagerState.currentPage,
                selectedTabPosition = selectedTabPosition,
                badgeMap = unreadMap,
                onTabSelected = { index ->
                    if (index != pagerState.currentPage) {
                        scope.launch { pagerState.scrollToPage(index) }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        MainTabPager(pagerState, innerPadding, backStack)
    }

    ProfileLoadingOverlay(profileViewModel)
}

@Composable
private fun ProfileLoadingOverlay(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoadingDialog(uiState.isLoading)
}

@Composable
private fun MainTabPager(
    pagerState: PagerState,
    innerPadding: PaddingValues,
    backStack: NavBackStack<NavKey>
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        beyondViewportPageCount = 1
    ) { page ->
        when (MainTab.entries[page]) {
            MainTab.Chats -> ChatListScreen { backStack.add(NavigationKey.ChatSession(it)) }
            MainTab.Contacts -> ContactListScreen(
                onNewFriends = { backStack.add(NavigationKey.NewFriends) },
                onGroups = { backStack.add(NavigationKey.GroupList) },
                onTags = { backStack.add(NavigationKey.ContactTags) },
                onDetail = { backStack.add(NavigationKey.ContactDetail(it)) },
                onProfileEdit = { backStack.add(NavigationKey.EditContactProfile(it)) }
            )

            MainTab.Discovery -> DiscoveryScreen(
                onMoments = { backStack.add(NavigationKey.Moments) },
                onIntercom = { backStack.add(NavigationKey.IntercomLobby) }
            )

            MainTab.Me -> MeScreen(backStack)
        }
    }
}
