package top.chengdongqing.wechat.features.chat.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadMoreType
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.rememberCallLauncher
import top.chengdongqing.wechat.features.call.ui.startCall
import top.chengdongqing.wechat.features.chat.ui.session.components.TimeDivider
import top.chengdongqing.wechat.features.chat.ui.session.input.InputBar
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageItem
import top.chengdongqing.wechat.features.chat.ui.session.util.KeyboardScrollEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.LoadMoreEffect
import top.chengdongqing.wechat.features.chat.ui.session.util.MessageDataScrollEffect

@OptIn(ExperimentalPermissionsApi::class)
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

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val overscrollEffect = rememberBounceOverscrollEffect()

    // 键盘和数据更新时的自动滚动
    KeyboardScrollEffect(listState, messages.size)
    MessageDataScrollEffect(listState, messages)

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
    val chatContext = rememberChatSessionContext(
        viewModel = viewModel,
        uiState = uiState,
        onPreviewFile = onNavigateToFilePreview,
        onLaunchCall = launchCall,
        onNavigateToContact = { isPeer ->
            val id = if (isPeer) uiState.peerId else uiState.myId
            onNavigateToContact(id!!)
        },
        onNavigateToRequestAddFriend = onNavigateToRequestAddFriend,
        onNavigateToWebView = onNavigateToWebView
    )

    // 注册与清除当前聚焦的session
    LifecycleResumeEffect(chatId) {
        viewModel.activeSessionManager.enter(chatId)

        onPauseOrDispose {
            viewModel.activeSessionManager.leave()
        }
    }

    CompositionLocalProvider(LocalChatSessionContext provides chatContext) {
        Box {
            // 聊天背景图片
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
                        MessageItem(
                            message = message,
                            peerAvatar = uiState.peerAvatar,
                            myAvatar = uiState.myAvatar
                        )
                        TimeDivider(messages, index)
                    }

                    // 加载更多指示器
                    if (uiState.isLoadingMore) {
                        item(key = "load_more") {
                            WeLoadMore(type = LoadMoreType.Loading)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSessionTopBar(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle(0)

    WeTopBar(
        titleContent = {
            ChatSessionTitle(viewModel, uiState)
        },
        onBack = onBack,
        unreadCount = unreadCount
    ) {
        ActionIcon(iconResId = R.drawable.ic_more_outlined, description = "更多") {
            onNavigateToInfo()
        }
    }
}

@Composable
private fun ChatSessionTitle(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
) {
    val isE2EActive by viewModel.isE2EActive.collectAsStateWithLifecycle()
    val statusColor =
        if (uiState.isOnline) WeTheme.colorScheme.primary else WeTheme.colorScheme.divider
    val statusDesc = if (uiState.isOnline) "在线" else "离线"

    Row(
        modifier = Modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 名字
        Text(
            text = uiState.title,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = WeTheme.colorScheme.textPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        if (!uiState.isMyself) {
            // 加密锁图标
            if (isE2EActive) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_filled),
                    contentDescription = "已加密",
                    modifier = Modifier.size(16.dp),
                    tint = WeTheme.colorScheme.textSecondary
                )
            }

            // 在线状态小圆点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .semantics { contentDescription = statusDesc }
                    .background(statusColor, CircleShape)
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.4f),
                        CircleShape
                    )
            )
        }
    }
}