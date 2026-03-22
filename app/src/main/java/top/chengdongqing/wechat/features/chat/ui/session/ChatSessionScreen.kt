package top.chengdongqing.wechat.features.chat.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadMoreType
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.rememberCallLauncher
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.features.call.ui.startCall
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.components.ChatSessionTopBar
import top.chengdongqing.wechat.features.chat.ui.session.components.MultiSelectBottomBar
import top.chengdongqing.wechat.features.chat.ui.session.components.TimeDivider
import top.chengdongqing.wechat.features.chat.ui.session.input.InputBar
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageItem
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.features.chat.ui.session.message.toolbar.MessageToolbar
import top.chengdongqing.wechat.features.chat.ui.session.peer.PeerDeviceOverlay
import top.chengdongqing.wechat.features.chat.ui.session.util.KeyboardScrollEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.LoadMoreEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.MessageDataScrollEffect
import top.chengdongqing.wechat.features.contacts.ui.picker.rememberPickContactLauncher

@Composable
fun ChatSessionScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToContact: (id: String) -> Unit,
    onNavigateToFilePreview: (messageId: String) -> Unit,
    onNavigateToMusicPreview: (messageId: String, trackName: String) -> Unit,
    onNavigateToRequestAddFriend: () -> Unit,
    onNavigateToWebView: (url: String) -> Unit,
    viewModel: ChatSessionViewModel = hiltViewModel { factory: ChatSessionViewModel.Factory ->
        factory.create(chatId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionRequired by viewModel.connectionRequired.collectAsStateWithLifecycle()
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val showPeerOverlay = remember { mutableStateOf(false) }

    LaunchedEffect(connectionRequired, connectionMode, uiState.isSelf) {
        if (uiState.isSelf == null || uiState.isSelf == true) return@LaunchedEffect

        val shouldShow = when {
            connectionRequired != null -> true
            // 蓝牙设备，如果保存过，在发送消息时将自动发起连接，不用弹窗
            connectionMode == ConnectionMode.Bluetooth && !viewModel.isBluetoothDeviceSaved() -> false
            // Wi-Fi p2p设备，每次都要重新连接
            connectionMode == ConnectionMode.WiFiDirect && !viewModel.isConnected() -> true
            else -> false
        }

        if (shouldShow) {
            showPeerOverlay.value = true
        }
    }

    LaunchedEffect(uiState.isOnline) {
        if (uiState.isOnline && showPeerOverlay.value) {
            showPeerOverlay.value = false
        }
    }

    PeerDeviceOverlay(
        visible = showPeerOverlay.value,
        userId = chatId,
        mode = connectionMode,
        onConnected = { showPeerOverlay.value = false },
        onClose = { showPeerOverlay.value = false }
    )

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val toolbarState by viewModel.toolbarState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val resources = LocalResources.current
    val listState = rememberLazyListState()
    val overscrollEffect = rememberBounceOverscrollEffect()
    val dialog = rememberDialogState()

    /**
     * 键盘和数据更新时的自动滚动
     */
    KeyboardScrollEffect(listState, messages.size)
    MessageDataScrollEffect(listState, messages)

    /**
     * 上拉加载更多的监听
     */
    LoadMoreEffect(
        listState = listState,
        messages = messages,
        isLoadingMore = uiState.isLoadingMore,
        hasMoreMessages = uiState.hasMoreMessages,
        onLoadMore = { viewModel.loadMore() }
    )

    /**
     * 调起通话
     */
    val launchCall = rememberCallLauncher(chatId) { id, type ->
        context.startCall(id, type)
    }

    /**
     * 上下文
     */
    val chatContext = rememberChatSessionContext(
        viewModel = viewModel,
        uiState = uiState,
        onNavigateToContact = { isPeer ->
            val id = if (isPeer) uiState.peerId else uiState.myId
            onNavigateToContact(id!!)
        },
        onNavigateToRequestAddFriend = onNavigateToRequestAddFriend,
        onNavigateToWebView = onNavigateToWebView
    )

    /**
     * 生命周期感知
     */
    LifecycleResumeEffect(chatId) {
        // 注册当前会话为聚焦的会话
        viewModel.activeSessionManager.enter(chatId)
        // 清除消息未读状态
        viewModel.clearUnreadState()

        onPauseOrDispose {
            // 清除当前会话的聚焦状态
            viewModel.activeSessionManager.leave()
            // 切到后台自动停止播放语音
            viewModel.stopVoice()
        }
    }

    val pickContact = rememberPickContactLauncher { contacts ->
        dialog.show(
            title = resources.getString(R.string.msg_confirm_forward, contacts.size)
        ) {
            viewModel.forwardMessages(contacts.map { it.id }.toSet())
        }
    }

    /**
     * UI事件处理
     */
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is MessageUiEvent.ShowDeleteConfirm -> {
                    dialog.show(
                        title = resources.getString(R.string.msg_confirm_delete),
                        okText = R.string.action_delete,
                        okColor = Danger
                    ) {
                        if (event.messageId != null) {
                            viewModel.deleteMessage(event.messageId)
                        } else {
                            viewModel.deleteSelectedMessages()
                        }
                    }
                }

                is MessageUiEvent.ShowDownloadConfirm -> {
                    dialog.show(
                        title = resources.getString(R.string.msg_confirm_save),
                        okText = R.string.action_save
                    ) {
                        viewModel.saveSelectedMessageFiles()
                    }
                }

                is MessageUiEvent.ForwardMessage -> {
                    event.messageId?.let { id ->
                        viewModel.toggleMessageSelection(id)
                    }
                    pickContact(99)
                }

                is MessageUiEvent.PreviewFile -> {
                    onNavigateToFilePreview(event.messageId)
                }

                is MessageUiEvent.PreviewMusic -> {
                    onNavigateToMusicPreview(event.messageId, event.trackName)
                }

                is MessageUiEvent.LaunchCall -> {
                    launchCall(event.callType)
                }

                is MessageUiEvent.NavigateToContact -> {
                    onNavigateToContact(event.contactId)
                }

                else -> {}
            }
        }
    }

    CompositionLocalProvider(LocalChatSessionContext provides chatContext) {
        Box {
            /**
             * 聊天背景图片
             */
            uiState.backgroundPath?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Scaffold(
                topBar = {
                    ChatSessionTopBar(
                        viewModel = viewModel,
                        uiState = uiState,
                        onBack = onBack,
                        onNavigateToInfo = onNavigateToInfo
                    )
                },
                bottomBar = {
                    if (!uiState.isSelectMode) {
                        InputBar(
                            viewModel = viewModel,
                            uiState = uiState,
                            listState = listState,
                            onLaunchCall = launchCall
                        )
                    } else {
                        MultiSelectBottomBar(
                            enabled = uiState.selectedCount > 0,
                            onActionClick = viewModel::handleMultiSelectAction,
                            onExitSelectMode = viewModel::exitSelectMode
                        )
                    }
                },
                containerColor = if (uiState.backgroundPath == null) WeTheme.colorScheme.background else Color.Unspecified
            ) { innerPadding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .overscroll(overscrollEffect),
                    contentPadding = PaddingValues(10.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.Top,
                    overscrollEffect = overscrollEffect
                ) {
                    itemsIndexed(
                        items = messages,
                        key = { _, message -> message.id }
                    ) { index, message ->
                        MessageItem(
                            message = message,
                            peerAvatar = uiState.peerAvatar,
                            myAvatar = uiState.myAvatar,
                            isSelectMode = uiState.isSelectMode,
                            isMessageSelected = uiState.isSelectMode
                                    && viewModel.isMessageSelected(message.id),
                            onMessageClick = {
                                if (!uiState.isSelectMode) {
                                    viewModel.handleMessageClick(message)
                                } else {
                                    viewModel.toggleMessageSelection(message.id)
                                }
                            },
                            onMessageLongPress = { bubblePosition, bubbleHeight ->
                                viewModel.handleMessageLongPress(
                                    message,
                                    bubblePosition,
                                    bubbleHeight
                                )
                            }
                        )

                        /**
                         * 时间分隔线
                         */
                        TimeDivider(messages, index)
                    }

                    /**
                     * 加载更多指示器
                     */
                    if (uiState.isLoadingMore) {
                        item(key = "load_more") {
                            WeLoadMore(type = LoadMoreType.Loading)
                        }
                    }
                }
            }

            MessageToolbar(
                visible = toolbarState.visible,
                actions = toolbarState.actions,
                bubblePosition = toolbarState.bubblePosition,
                bubbleHeight = toolbarState.bubbleHeight,
                isTextMessage = toolbarState.message?.content is MessageContent.Text,
                onActionClick = viewModel::handleToolbarAction,
                onDismiss = viewModel::dismissToolbar
            )
        }
    }

    LoadingDialog(uiState.isFullscreenLoading)
}