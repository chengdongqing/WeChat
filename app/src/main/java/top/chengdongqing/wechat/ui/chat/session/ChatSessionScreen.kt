package top.chengdongqing.wechat.ui.chat.session

import android.net.Uri
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.input.InputBar
import top.chengdongqing.wechat.ui.chat.session.message.MessageItem
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.util.rememberBounceOverscrollEffect

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
    val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
    val playingMessageId by viewModel.playingMessageId.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val overscrollEffect = rememberBounceOverscrollEffect()

    // 当键盘弹出时，消息列表自动置底
    KeyboardScrollEffect(listState, messages.size)
    // 当数据更新时，消息列表自动置底
    MessageDataScrollEffect(listState, messages)

    // 提供媒体上下文
    val mediaContext = remember(mediaList, playingMessageId) {
        MediaContext(
            allMedia = mediaList,
            getIndexOf = { content -> mediaList.indexOf(content) },
            playingMessageId = playingMessageId,
            onVoiceToggle = { id, uri -> viewModel.toggleVoicePlay(id, uri) },
            onVoiceStop = { if (playingMessageId != null) viewModel.stopVoice() }
        )
    }

    // 回到后台或当前页面销毁，停止播放语音
    VoicePlayingLifecycle {
        if (playingMessageId != null) viewModel.stopVoice()
    }

    CompositionLocalProvider(LocalMediaContext provides mediaContext) {
        Scaffold(
            topBar = {
                WeTopBar(title = uiState.title, onBack = onBack) {
                    ActionIcon(iconResId = R.drawable.ic_more_outlined, description = "更多") {
                        onNavigateToInfo()
                    }
                }
            },
            bottomBar = {
                InputBar(listState = listState, isSending = uiState.isSending) { content, onSent ->
                    viewModel.sendMessage(content) {
                        scope.launch {
                            delay(100)
                            listState.animateScrollToItem(0)
                            delay(100)
                            onSent?.invoke()

                            viewModel.finishScrollToLatest()
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
                reverseLayout = true, // 新消息在底部，旧消息在顶部；键盘弹出时列表会自动推上去
                verticalArrangement = Arrangement.Top,
                overscrollEffect = overscrollEffect
            ) {
                itemsIndexed(
                    items = messages,
                    key = { _, message -> message.id }
                ) { index, message ->
                    MessageItem(message)
                    TimeDivider(messages, index)
                }
            }
        }
    }
}

@Composable
private fun VoicePlayingLifecycle(onVoiceStop: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(Unit) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onVoiceStop()
            }
        }
        lifecycle.addObserver(lifecycleObserver)

        onDispose {
            onVoiceStop()
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
}

data class MediaContext(
    val allMedia: List<MessageContent.Media>,
    val getIndexOf: (MessageContent.Media) -> Int,
    val playingMessageId: String?,
    val onVoiceToggle: (messageId: String, uri: Uri) -> Unit,
    val onVoiceStop: () -> Unit
)

val LocalMediaContext = compositionLocalOf<MediaContext?> { null }