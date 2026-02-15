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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadMoreType
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.components.TimeDivider
import top.chengdongqing.wechat.features.chat.ui.session.input.InputBar
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageItem
import top.chengdongqing.wechat.features.chat.ui.session.util.KeyboardScrollEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.LoadMoreEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.MessageDataScrollEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.VoicePlayingLifecycle

@Composable
fun ChatSessionScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    viewModel: ChatSessionViewModel = hiltViewModel { factory: ChatSessionViewModel.Factory ->
        factory.create(chatId)
    }
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
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

    // 媒体上下文
    val mediaContext = rememberMediaContext(viewModel)

    CompositionLocalProvider(LocalMediaContext provides mediaContext) {
        Scaffold(
            topBar = {
                ChatSessionTopBar(
                    title = uiState.title,
                    onBack = onBack,
                    onNavigateToInfo = onNavigateToInfo
                )
            },
            bottomBar = {
                InputBar(listState = listState, isSending = uiState.isSending) { content, onSent ->
                    viewModel.sendMessage(content) {
                        scope.launch {
                            delay(100)
                            listState.animateScrollToItem(0)
                            delay(100)
                            onSent?.invoke()
                            viewModel.finishSending()
                        }
                    }
                }
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
private fun rememberMediaContext(viewModel: ChatSessionViewModel): MediaContext {
    val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
    val playingMessageId by viewModel.playingMessageId.collectAsStateWithLifecycle()

    // 生命周期感知的语音播放控制
    VoicePlayingLifecycle {
        if (playingMessageId != null) {
            viewModel.stopVoice()
        }
    }

    return remember(mediaList, playingMessageId) {
        MediaContext(
            allMedia = mediaList,
            getIndexOf = { content -> mediaList.indexOf(content) },
            playingMessageId = playingMessageId,
            onVoiceToggle = { id, localPath -> viewModel.toggleVoicePlay(id, localPath) },
            onVoiceStop = { if (playingMessageId != null) viewModel.stopVoice() }
        )
    }
}

/**
 * 媒体上下文数据类
 */
data class MediaContext(
    val allMedia: List<MessageContent.Media>,
    val getIndexOf: (MessageContent.Media) -> Int,
    val playingMessageId: String?,
    val onVoiceToggle: (messageId: String, localPath: String) -> Unit,
    val onVoiceStop: () -> Unit
)

val LocalMediaContext = compositionLocalOf<MediaContext?> { null }