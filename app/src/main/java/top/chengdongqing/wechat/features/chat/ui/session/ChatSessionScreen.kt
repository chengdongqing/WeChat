package top.chengdongqing.wechat.features.chat.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadMoreType
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.rememberCallLauncher
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.call.ui.startCall
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.components.TimeDivider
import top.chengdongqing.wechat.features.chat.ui.session.input.InputBar
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageItem
import top.chengdongqing.wechat.features.chat.ui.session.util.KeyboardScrollEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.LoadMoreEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.MessageDataScrollEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.VoicePlayingLifecycle

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatSessionScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToFilePreview: (messageId: String) -> Unit,
    viewModel: ChatSessionViewModel = hiltViewModel { factory: ChatSessionViewModel.Factory ->
        factory.create(chatId)
    }
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val overscrollEffect = rememberBounceOverscrollEffect()

    // 键盘和数据更新时的自动滚动
    KeyboardScrollEffect(listState, messages.size)
    MessageDataScrollEffect(listState, messages)

    // 初始加载完成后滚动到底部
    LaunchedEffect(uiState.shouldScrollToBottom) {
        if (uiState.shouldScrollToBottom) {
            listState.scrollToItem(0)
            viewModel.onScrolledToBottomHandled()
        }
    }

    // 上拉加载更多的监听
    LoadMoreEffect(
        listState = listState,
        messages = messages,
        isLoadingMore = uiState.isLoadingMore,
        hasMoreMessages = uiState.hasMoreMessages,
        onLoadMore = { viewModel.loadMore() }
    )

    // 调起通话
    val launchCall = rememberCallLauncher(chatId) { id, type ->
        context.startCall(id, type)
    }

    // 上下文
    val chatContext = rememberChatContext(
        viewModel = viewModel,
        uiState = uiState,
        onPreviewFile = onNavigateToFilePreview,
        onLaunchCall = launchCall
    )

    CompositionLocalProvider(LocalChatContext provides chatContext) {
        Scaffold(
            topBar = {
                ChatSessionTopBar(
                    title = uiState.title,
                    onBack = onBack,
                    onNavigateToInfo = onNavigateToInfo
                )
            },
            bottomBar = {
                InputBar(
                    listState = listState,
                    isSending = uiState.isSending,
                    onSendMessage = { content, onSent ->
                        viewModel.sendMessage(content) {
                            scope.launch {
                                delay(100)
                                listState.animateScrollToItem(0)
                                delay(100)
                                onSent?.invoke()
                                viewModel.finishSending()
                            }
                        }
                    },
                    onLaunchCall = launchCall
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF3F3F3))
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
                        myAvatar = uiState.myAvatar
                    )
                    TimeDivider(messages, index)
                }

                // 加载更多指示器
                if (uiState.hasMoreMessages) {
                    item(key = "load_more") {
                        WeLoadMore(type = LoadMoreType.Loading)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSessionTopBar(title: String, onBack: () -> Unit, onNavigateToInfo: () -> Unit) {
    WeTopBar(title = title, onBack = onBack) {
        ActionIcon(iconResId = R.drawable.ic_more_outlined, description = "更多") {
            onNavigateToInfo()
        }
    }
}

@Composable
private fun rememberChatContext(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
    onPreviewFile: (messageId: String) -> Unit,
    onLaunchCall: (type: CallType) -> Unit
): ChatContext {
    val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
    val playingMessageId by viewModel.playingMessageId.collectAsStateWithLifecycle()

    // 生命周期感知的语音播放控制
    VoicePlayingLifecycle {
        if (playingMessageId != null) {
            viewModel.stopVoice()
        }
    }

    return remember(mediaList, playingMessageId, uiState.isMyself) {
        ChatContext(
            isMyself = uiState.isMyself,
            mediaList = mediaList,
            getMediaIndexOf = { content -> mediaList.indexOf(content) },
            playingVoiceId = playingMessageId,
            onVoiceToggle = { id, localPath -> viewModel.toggleVoicePlay(id, localPath) },
            onVoiceStop = { if (playingMessageId != null) viewModel.stopVoice() },
            onRetrySend = { viewModel.retrySend(it) },
            onPreviewFile = { onPreviewFile(it) },
            onNavigateToCall = onLaunchCall
        )
    }
}

/**
 * 上下文定义
 */
data class ChatContext(
    val isMyself: Boolean,
    val mediaList: List<MessageContent.Media>,
    val getMediaIndexOf: (MessageContent.Media) -> Int,
    val playingVoiceId: String?,
    val onVoiceToggle: (messageId: String, localPath: String) -> Unit,
    val onVoiceStop: () -> Unit,
    val onRetrySend: (messageId: String) -> Unit,
    val onPreviewFile: (messageId: String) -> Unit,
    val onNavigateToCall: (type: CallType) -> Unit
)

val LocalChatContext = compositionLocalOf<ChatContext?> { null }