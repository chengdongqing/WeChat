package top.chengdongqing.wechat.app.shell

import androidx.activity.compose.BackHandler
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
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatHistoryPayload
import top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar.WeNavigationBottomBar
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.list.ChatListScreen
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionScreen
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

    val containerPagerState = rememberPagerState(1) { 2 }
    HorizontalPager(containerPagerState) { page ->
        when (page) {
            1 -> {
                Scaffold(
                    topBar = {
                        MainTopBar(
                            currentTab = currentTab,
                            unreadMap = unreadMap,
                            onGroupChat = { backStack.add(NavigationKey.GroupChat("")) },
                            onAddFriend = { backStack.add(NavigationKey.AddFriend) },
                            onPayment = { backStack.add(NavigationKey.PaymentCode) },
                            onScannedQrCode = profileViewModel::handleScannedQRCode,
                            onChatWithAI = {
                                scope.launch { containerPagerState.animateScrollToPage(0) }
                            }
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
                    MainTabPager(
                        pagerState = pagerState,
                        innerPadding = innerPadding,
                        backStack = backStack,
                    )
                }

                ProfileLoadingOverlay(profileViewModel)
            }

            0 -> {
                val chatId = LocalAiAssistant.ID

                BackHandler(containerPagerState.currentPage == 0) {
                    scope.launch {
                        containerPagerState.animateScrollToPage(1)
                    }
                }

                ChatTheme {
                    ChatSessionScreen(
                        chatId = chatId,
                        isSpecialPage = true,
                        onBack = {
                            scope.launch {
                                containerPagerState.animateScrollToPage(1)
                            }
                        },
                        onInfo = {
                            backStack.add(NavigationKey.ChatInfo(chatId))
                        },
                        onContact = { id ->
                            backStack.removeIf { key -> key is NavigationKey.ContactDetail }
                            backStack.add(NavigationKey.ContactDetail(id))
                        },
                        onFilePreview = { id -> backStack.add(NavigationKey.FilePreview(id)) },
                        onMusicPreview = { id, name ->
                            backStack.add(
                                NavigationKey.MusicPreview(
                                    messageId = id,
                                    trackName = name
                                )
                            )
                        },
                        onRequestAddFriend = { },
                        onWebView = { url -> backStack.add(NavigationKey.WebView(url)) },
                        onFavorites = {
                            backStack.add(NavigationKey.Favorites(chatId))
                        },
                        onChatHistory = { history ->
                            backStack.add(
                                NavigationKey.ChatHistory(
                                    Json.encodeToString(
                                        ChatHistoryPayload(
                                            history.title,
                                            history.items
                                        )
                                    )
                                )
                            )
                        },
                        onLive = { _, _, _ -> },
                        onLiveLocation = {}
                    )
                }
            }
        }
    }
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
            .fillMaxSize()
            .padding(innerPadding),
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
