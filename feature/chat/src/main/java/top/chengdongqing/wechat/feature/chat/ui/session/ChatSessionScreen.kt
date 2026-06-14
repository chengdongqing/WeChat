package top.chengdongqing.wechat.feature.chat.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadMoreType
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.theme.RedDanger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.rememberCallLauncher
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.feature.call.ui.startCall
import top.chengdongqing.wechat.feature.chat.data.mapper.toMessageType
import top.chengdongqing.wechat.feature.chat.ui.session.components.ChatSessionTopBar
import top.chengdongqing.wechat.feature.chat.ui.session.components.MultiSelectBottomBar
import top.chengdongqing.wechat.feature.chat.ui.session.components.TimeDivider
import top.chengdongqing.wechat.feature.chat.ui.session.input.InputBar
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageItem
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.feature.chat.ui.session.message.toolbar.MessageToolbar
import top.chengdongqing.wechat.feature.chat.ui.session.peer.PeerDeviceOverlay
import top.chengdongqing.wechat.feature.chat.ui.session.util.KeyboardScrollEffect
import top.chengdongqing.wechat.feature.chat.ui.session.util.LoadMoreEffect
import top.chengdongqing.wechat.feature.chat.ui.session.util.MessageDataScrollEffect
import top.chengdongqing.wechat.feature.contacts.ui.picker.rememberPickContactLauncher

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
    viewModel: ChatSessionViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val toolbarState by viewModel.toolbarState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val launchCall = rememberCallLauncher(chatId) { id, type -> context.startCall(id, type) }
    val chatContext = rememberChatSessionContext(
        viewModel = viewModel,
        uiState = uiState,
        onNavigateToContact = { isPeer ->
            onNavigateToContact(if (isPeer) uiState.peerId!! else uiState.myId!!)
        },
        onNavigateToRequestAddFriend = onNavigateToRequestAddFriend,
        onNavigateToWebView = onNavigateToWebView
    )

    KeyboardScrollEffect(listState, messages.size)
    MessageDataScrollEffect(listState, messages)
    LoadMoreEffect(
        listState = listState,
        messages = messages,
        isLoadingMore = uiState.isLoadingMore,
        hasMoreMessages = uiState.hasMoreMessages,
        onLoadMore = viewModel::loadMore
    )
    LifecycleResumeEffect(chatId) {
        viewModel.onEnterSession()
        viewModel.clearUnreadState()
        onPauseOrDispose {
            viewModel.onLeaveSession()
            viewModel.stopVoice()
        }
    }

    PeerConnectionOverlay(chatId, uiState, viewModel)
    ChatSessionUiEventHandler(
        viewModel = viewModel,
        launchCall = launchCall,
        onNavigateToContact = onNavigateToContact,
        onNavigateToFilePreview = onNavigateToFilePreview,
        onNavigateToMusicPreview = onNavigateToMusicPreview
    )

    CompositionLocalProvider(LocalChatSessionContext provides chatContext) {
        Box {
            uiState.backgroundPath?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Scaffold(
                topBar = { ChatSessionTopBar(viewModel, uiState, onBack, onNavigateToInfo) },
                bottomBar = {
                    if (!uiState.isSelectMode) {
                        InputBar(viewModel, uiState, listState, launchCall)
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
                ChatMessageList(messages, uiState, viewModel, listState, innerPadding)
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

/**
 * 管理点对点连接弹窗的显示逻辑
 */
@Composable
private fun PeerConnectionOverlay(
    chatId: String,
    uiState: ChatSessionUiState,
    viewModel: ChatSessionViewModel
) {
    val connectionRequired by viewModel.connectionRequired.collectAsStateWithLifecycle()
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    var showOverlay by remember { mutableStateOf(false) }
    val closeOverlay = { showOverlay = false }

    LaunchedEffect(connectionRequired, connectionMode, uiState.isSelf) {
        if (uiState.isSelf == null || uiState.isSelf) {
            return@LaunchedEffect
        }

        val shouldShow = when {
            connectionRequired != null -> true
            // 蓝牙设备若已保存，发送时自动连接，无需弹窗
            connectionMode == ConnectionMode.Bluetooth && !viewModel.isBluetoothDeviceSaved() -> false
            // Wi-Fi Direct 每次都需要重新连接
            connectionMode == ConnectionMode.WiFiDirect && !viewModel.isConnected() -> true
            else -> false
        }
        if (shouldShow) {
            showOverlay = true
        }
    }

    LaunchedEffect(uiState.isOnline) {
        if (uiState.isOnline) {
            closeOverlay()
        }
    }

    PeerDeviceOverlay(
        visible = showOverlay,
        userId = chatId,
        mode = connectionMode,
        onConnected = closeOverlay,
        onDismiss = closeOverlay
    )
}

/**
 * UI 事件处理
 */
@Composable
private fun ChatSessionUiEventHandler(
    viewModel: ChatSessionViewModel,
    launchCall: (CallType) -> Unit,
    onNavigateToContact: (String) -> Unit,
    onNavigateToFilePreview: (String) -> Unit,
    onNavigateToMusicPreview: (String, String) -> Unit,
) {
    val resources = LocalResources.current
    val dialog = rememberDialogState()
    val pickContact = rememberPickContactLauncher { contacts ->
        dialog.show(resources.getString(R.string.msg_confirm_forward, contacts.size)) {
            viewModel.forwardMessages(contacts.map { it.id }.toSet())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is MessageUiEvent.ShowDeleteConfirm -> dialog.show(
                    title = resources.getString(R.string.msg_confirm_delete),
                    okText = R.string.action_delete,
                    okColor = RedDanger
                ) {
                    if (event.messageId != null) viewModel.deleteMessage(event.messageId)
                    else viewModel.deleteSelectedMessages()
                }

                is MessageUiEvent.ShowDownloadConfirm -> dialog.show(
                    title = resources.getString(R.string.msg_confirm_save),
                    okText = R.string.action_save
                ) { viewModel.saveSelectedMessageFiles() }

                is MessageUiEvent.ForwardMessage -> pickContact(99)
                is MessageUiEvent.PreviewFile -> onNavigateToFilePreview(event.messageId)
                is MessageUiEvent.PreviewMusic -> onNavigateToMusicPreview(
                    event.messageId,
                    event.trackName
                )

                is MessageUiEvent.LaunchCall -> launchCall(event.callType)
                is MessageUiEvent.NavigateToContact -> onNavigateToContact(event.contactId)
                else -> {}
            }
        }
    }
}

/**
 * 消息列表
 */
@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    uiState: ChatSessionUiState,
    viewModel: ChatSessionViewModel,
    listState: LazyListState,
    innerPadding: PaddingValues
) {
    val overscrollEffect = rememberBounceOverscrollEffect()

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
            key = { _, message -> message.id },
            contentType = { _, message -> message.content.toMessageType() }
        ) { index, message ->
            MessageItem(
                message = message,
                peerAvatar = uiState.peerAvatar,
                myAvatar = uiState.myAvatar,
                isSelectMode = uiState.isSelectMode,
                isMessageSelected = uiState.isSelectMode && viewModel.isMessageSelected(message.id),
                onMessageClick = {
                    if (!uiState.isSelectMode) viewModel.handleMessageClick(message)
                    else viewModel.toggleMessageSelection(message.id)
                },
                onMessageLongPress = { pos, height ->
                    viewModel.handleMessageLongPress(message, pos, height)
                }
            )
            TimeDivider(messages, index)
        }

        if (uiState.isLoadingMore) {
            item(key = "load_more") {
                WeLoadMore(type = LoadMoreType.Loading)
            }
        }
    }
}