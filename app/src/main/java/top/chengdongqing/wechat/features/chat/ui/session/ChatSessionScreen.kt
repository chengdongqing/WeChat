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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadMoreType
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.rememberCallLauncher
import top.chengdongqing.wechat.features.call.ui.startCall
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.components.ChatSessionTopBar
import top.chengdongqing.wechat.features.chat.ui.session.components.TimeDivider
import top.chengdongqing.wechat.features.chat.ui.session.input.InputBar
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageItem
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.features.chat.ui.session.message.toolbar.MessageToolbar
import top.chengdongqing.wechat.features.chat.ui.session.util.KeyboardScrollEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.LoadMoreEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.MessageDataScrollEffect

@Composable
fun ChatSessionScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToContact: (id: String) -> Unit,
    onNavigateToFilePreview: (messageId: String) -> Unit,
    onNavigateToRequestAddFriend: () -> Unit,
    onNavigateToWebView: (url: String) -> Unit,
    viewModel: ChatSessionViewModel = hiltViewModel { factory: ChatSessionViewModel.Factory ->
        factory.create(chatId)
    }
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toolbarState by viewModel.toolbarState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val overscrollEffect = rememberBounceOverscrollEffect()

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

    val dialog = rememberDialogState()
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is MessageUiEvent.ShowDeleteConfirm -> {
                    dialog.show(
                        title = "确认删除？",
                        okText = "删除",
                        okColor = Danger
                    ) {
                        viewModel.deleteMessage(event.messageId)
                    }
                }

                is MessageUiEvent.EnterMultiSelectMode -> {

                }

                is MessageUiEvent.ForwardMessage -> {

                }

                is MessageUiEvent.PreviewFile -> {
                    onNavigateToFilePreview(event.messageId)
                }

                is MessageUiEvent.LaunchCall -> {
                    launchCall(event.callType)
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
                    InputBar(
                        viewModel = viewModel,
                        uiState = uiState,
                        listState = listState,
                        onLaunchCall = launchCall
                    )
                },
                containerColor = if (uiState.backgroundPath == null) Color(0xFFF3F3F3) else Color.Unspecified
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
                        val isCurrentMessage =
                            toolbarState.visible && toolbarState.message?.id == message.id

                        MessageItem(
                            message = message,
                            peerAvatar = uiState.peerAvatar,
                            myAvatar = uiState.myAvatar,
                            isTextSelectable = isCurrentMessage && toolbarState.textSelection != null,
                            textSelection = if (isCurrentMessage) toolbarState.textSelection else null,
                            onMessageClick = viewModel::handleMessageClick,
                            onMessageLongPress = viewModel::handleMessageLongPress,
                            onTextSelectionChange = viewModel::handleTextSelectionChange,
                            onTextSelectionDismiss = viewModel::dismissToolbar
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
                position = toolbarState.position,
                bubblePosition = toolbarState.bubblePosition,
                bubbleHeight = toolbarState.bubbleHeight,
                isTextMessage = toolbarState.message?.content is MessageContent.Text,
                onActionClick = viewModel::handleToolbarAction,
                onDismiss = viewModel::dismissToolbar
            )
        }
    }
}