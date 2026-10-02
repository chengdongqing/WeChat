package top.chengdongqing.wechat.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatHistoryPayload
import top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar.WeNavigationBottomBar
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.navigation.LocalAppNavigator
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.core.qrcode.scanner.rememberQrCodeScannerLauncher
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.list.ChatListRoute
import top.chengdongqing.wechat.feature.chat.ui.list.ChatListScreen
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionScreen
import top.chengdongqing.wechat.feature.contacts.ui.list.ContactListRoute
import top.chengdongqing.wechat.feature.contacts.ui.list.ContactListScreen
import top.chengdongqing.wechat.feature.discovery.DiscoverRoute
import top.chengdongqing.wechat.feature.discovery.DiscoverScreen
import top.chengdongqing.wechat.feature.profile.ui.MeRoute
import top.chengdongqing.wechat.feature.profile.ui.MeScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.HandleProfileNavigationEvents
import top.chengdongqing.wechat.feature.profile.ui.profile.ProfileViewModel

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current
    val containerPagerState = rememberPagerState(1) { 2 }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val qrCodeScanner = rememberQrCodeScannerLauncher { qrCodes ->
        qrCodes.firstOrNull()?.let(profileViewModel::handleScannedQRCode)
    }

    val actions = remember(navigator) {
        HomeActions(
            onQuickActionClick = { type ->
                when (type) {
                    QuickActionType.ScanCode -> {
                        qrCodeScanner.launch()
                    }

                    QuickActionType.AddFriend -> {
                        navigator.navigateTo(ScreenRoute.AddFriend)
                    }

                    QuickActionType.Money -> {
                        navigator.navigateTo(ScreenRoute.Money)
                    }

                    else -> Unit
                }
            },
            onTabSelected = viewModel::onTabSelected,
            onChatWithAI = {
                scope.launch {
                    containerPagerState.animateScrollToPage(0)
                }
            }
        )
    }

    HomeScreen(
        state = state,
        actions = actions,
        containerPagerState = containerPagerState,
        snackbarHostState = snackbarHostState,
        chatsTab = { ChatListRoute() },
        contactsTab = { ContactListRoute() },
        discoverTab = { DiscoverRoute() },
        meTab = { MeRoute() }
    )

    ProfileLoadingOverlay(profileViewModel)

    HandleProfileNavigationEvents(
        viewModel = profileViewModel,
        snackbarHostState = snackbarHostState,
        onContactDetail = { navigator.navigateTo(ScreenRoute.ContactDetail(it)) },
        onPlainText = { navigator.navigateTo(ScreenRoute.PlainText(it)) },
        onWebView = { navigator.navigateTo(ScreenRoute.WebView(it)) }
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState = HomeUiState(),
    actions: HomeActions = HomeActions(),
    containerPagerState: PagerState = rememberPagerState(1) { 2 },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    chatsTab: @Composable () -> Unit,
    contactsTab: @Composable () -> Unit,
    discoverTab: @Composable () -> Unit,
    meTab: @Composable () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { HomeTab.entries.size })
    val selectedTabPosition by remember {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        actions.onTabSelected(HomeTab.entries[pagerState.currentPage])
    }

    HorizontalPager(containerPagerState) { page ->
        when (page) {
            1 -> {
                Scaffold(
                    topBar = {
                        HomeTopBar(
                            currentTab = state.currentTab,
                            unreadMap = state.unreadMap,
                            actions = actions
                        )
                    },
                    bottomBar = {
                        WeNavigationBottomBar(
                            tabs = HomeTab.entries,
                            currentTabIndex = pagerState.currentPage,
                            selectedTabPosition = selectedTabPosition,
                            badgeMap = state.unreadMap,
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
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (HomeTab.entries[page]) {
                            HomeTab.Chats -> chatsTab()
                            HomeTab.Contacts -> contactsTab()
                            HomeTab.Discover -> discoverTab()
                            HomeTab.Me -> meTab()
                        }
                    }
                }
            }

            0 -> {
                AiChat(containerPagerState)
            }
        }
    }
}

@Composable
private fun AiChat(
    containerPagerState: PagerState
) {
    val chatId = LocalAiAssistant.ID
    val scope = rememberCoroutineScope()
    val navigator = LocalAppNavigator.current

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
                navigator.navigateTo(ScreenRoute.ChatInfo(chatId))
            },
            onContact = { id ->
                navigator.backStack.removeIf { key -> key is ScreenRoute.ContactDetail }
                navigator.navigateTo(ScreenRoute.ContactDetail(id))
            },
            onFilePreview = { id ->
                navigator.navigateTo(ScreenRoute.FilePreview(id))
            },
            onMusicPreview = { id, name ->
                navigator.navigateTo(
                    ScreenRoute.MusicPreview(
                        messageId = id,
                        trackName = name
                    )
                )
            },
            onRequestAddFriend = { },
            onWebView = { url -> navigator.navigateTo(ScreenRoute.WebView(url)) },
            onFavorites = {
                navigator.navigateTo(ScreenRoute.Favorites(chatId))
            },
            onChatHistory = { history ->
                navigator.navigateTo(
                    ScreenRoute.ChatHistory(
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

@Composable
private fun ProfileLoadingOverlay(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoadingDialog(uiState.isLoading)
}

@Preview
@Composable
private fun HomePreview() {
    WeTheme {
        HomeScreen(
            chatsTab = { ChatListScreen() },
            contactsTab = { ContactListScreen() },
            discoverTab = { DiscoverScreen() },
            meTab = { MeScreen() },
        )
    }
}