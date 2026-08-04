package top.chengdongqing.wechat.feature.chat.ui.session

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.media.editor.ImageEditor
import top.chengdongqing.wechat.core.common.media.model.MediaItem
import top.chengdongqing.wechat.core.common.media.preview.WeMediaPreview
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.WeActionSheet
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadMoreType
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.SemanticError
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.model.MessageSendStatus
import top.chengdongqing.wechat.core.navigation.LocalCallLauncher
import top.chengdongqing.wechat.core.navigation.LocalContactPickerLauncher
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.feature.chat.data.mapper.toMessageType
import top.chengdongqing.wechat.feature.chat.ui.session.effect.BombMessageEffect
import top.chengdongqing.wechat.feature.chat.ui.session.effect.FestiveEffectEvent
import top.chengdongqing.wechat.feature.chat.ui.session.effect.FestiveEffectType
import top.chengdongqing.wechat.feature.chat.ui.session.effect.FestiveMessageEffect
import top.chengdongqing.wechat.feature.chat.ui.session.effect.bombShakeTransform
import top.chengdongqing.wechat.feature.chat.ui.session.input.InputBar
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageItem
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageToolbarState
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.LocalExpandedMediaAlbums
import top.chengdongqing.wechat.feature.chat.ui.session.message.toolbar.MessageToolbar
import top.chengdongqing.wechat.feature.chat.ui.session.peer.PeerDeviceOverlay
import top.chengdongqing.wechat.feature.chat.ui.session.util.KeyboardScrollEffect
import top.chengdongqing.wechat.feature.chat.ui.session.util.MessageDataScrollEffect

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
    onNavigateToLive: (liveId: String, isHost: Boolean, hostId: String) -> Unit,
    onNavigateToLiveLocation: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToChatHistory: (MessageContent.ChatHistory) -> Unit,
    viewModel: ChatSessionViewModel
) {
    val expandedMediaAlbums = remember(chatId) { mutableStateListOf<String>() }
    var mediaPreview by remember { mutableStateOf<ChatMediaPreviewState?>(null) }
    var editingImageUri by remember { mutableStateOf<Uri?>(null) }
    var editedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showEditedImageActions by remember { mutableStateOf(false) }
    var mediaPreviewClosing by remember { mutableStateOf(false) }
    val mediaPreviewScope = rememberCoroutineScope()
    val closeMediaPreview: () -> Unit = {
        if (!mediaPreviewClosing) {
            mediaPreviewClosing = true
            mediaPreviewScope.launch {
                // 先让视频 Surface 被封面替换并至少完成一次绘制，再触发共享元素退出。
                withFrameNanos { }
                withFrameNanos { }
                mediaPreview = null
                mediaPreviewClosing = false
            }
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val streamingAiMessage by viewModel.streamingAiMessage.collectAsStateWithLifecycle()
    val lazyMessageItems = viewModel.messagePagingFlow.collectAsLazyPagingItems()
    val toolbarState by viewModel.toolbarState.collectAsStateWithLifecycle()
    val liveLocationRoom by viewModel.liveLocationRoom.collectAsStateWithLifecycle()
    val selectingTextMessageId = toolbarState.message
        ?.takeIf { toolbarState.visible && it.content is MessageContent.Text }
        ?.id
    val listState = rememberLazyListState()
    var bombTrigger by remember(chatId) { mutableIntStateOf(0) }
    var bombProgress by remember(chatId) { mutableFloatStateOf(1f) }
    var festiveSerial by remember(chatId) { mutableIntStateOf(0) }
    var festiveEvent by remember(chatId) { mutableStateOf<FestiveEffectEvent?>(null) }
    val knownMessageIds = remember(chatId) { mutableSetOf<String>() }
    var messageSnapshotInitialized by remember(chatId) { mutableStateOf(false) }
    val launchCall = LocalCallLauncher.current.rememberLauncher(chatId)
    val sendEditedImageToContacts =
        LocalContactPickerLauncher.current.rememberLauncher { contacts ->
            editedImageUri?.let {
                viewModel.sendEditedImage(
                    it,
                    contacts.map { contact -> contact.id }.toSet()
                )
            }
            editedImageUri = null
        }
    val chatContext = rememberChatSessionContext(
        viewModel = viewModel,
        uiState = uiState,
        onNavigateToContact = { isPeer ->
            onNavigateToContact(if (isPeer) uiState.peerId!! else uiState.myId!!)
        },
        onNavigateToRequestAddFriend = onNavigateToRequestAddFriend,
        onNavigateToWebView = onNavigateToWebView,
        onNavigateToLive = onNavigateToLive
    )

    KeyboardScrollEffect(listState, lazyMessageItems.itemCount)
    MessageDataScrollEffect(
        listState = listState,
        messages = lazyMessageItems.itemSnapshotList.items,
        transientMessageId = streamingAiMessage?.id
    )
    LifecycleResumeEffect(chatId) {
        viewModel.onEnterSession()
        viewModel.clearUnreadState()
        onPauseOrDispose {
            viewModel.onLeaveSession()
            viewModel.stopVoice()
        }
    }

    val initialMessagesLoaded = lazyMessageItems.loadState.refresh is LoadState.NotLoading
    LaunchedEffect(lazyMessageItems.itemSnapshotList, initialMessagesLoaded) {
        val messages = lazyMessageItems.itemSnapshotList.items
        viewModel.syncMessages(messages)
        if (!messageSnapshotInitialized) {
            knownMessageIds += messages.map { it.id }
            messageSnapshotInitialized = initialMessagesLoaded
        } else {
            val freshMessages = messages.filter { knownMessageIds.add(it.id) }
            if (freshMessages.any { it.isBombMessage() }) {
                bombTrigger++
            }
            freshMessages.asReversed().firstNotNullOfOrNull { it.festiveEffectType() }?.let { type ->
                festiveSerial++
                festiveEvent = FestiveEffectEvent(festiveSerial, type)
            }
        }
    }
    LaunchedEffect(lazyMessageItems.itemSnapshotList, streamingAiMessage) {
        val streaming = streamingAiMessage ?: return@LaunchedEffect
        if (!streaming.isGenerating) {
            val persistedText = lazyMessageItems.itemSnapshotList.items
                .firstOrNull { it.id == streaming.id }
                ?.content
                .let { it as? MessageContent.Text }
                ?.text
            if (persistedText == streaming.text) {
                viewModel.finishAiStreamHandoff(streaming.id)
            }
        }
    }

    PeerConnectionOverlay(chatId, uiState, viewModel)
    ChatSessionUiEventHandler(
        viewModel = viewModel,
        launchCall = launchCall,
        onNavigateToContact = onNavigateToContact,
        onNavigateToFilePreview = onNavigateToFilePreview,
        onNavigateToMusicPreview = onNavigateToMusicPreview,
        onNavigateToLiveLocation = onNavigateToLiveLocation,
        onPreviewMedia = {
            mediaPreviewClosing = false
            mediaPreview = it
        },
        onEditImage = { editingImageUri = it },
        onOpenChatHistory = onNavigateToChatHistory
    )

    CompositionLocalProvider(
        LocalChatSessionContext provides chatContext,
        LocalExpandedMediaAlbums provides expandedMediaAlbums
    ) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = mediaPreview,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "chat-media-preview"
            ) { preview ->
                CompositionLocalProvider(
                    LocalMediaSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalMediaAnimatedVisibilityScope provides this@AnimatedContent
                ) {
                    if (preview == null) {
                        Box(
                            modifier = Modifier.then(
                                if (selectingTextMessageId != null) {
                                    Modifier.pointerInput(selectingTextMessageId) {
                                        awaitEachGesture {
                                            awaitFirstDown(
                                                requireUnconsumed = false,
                                                pass = PointerEventPass.Final
                                            )
                                            val up = waitForUpOrCancellation(PointerEventPass.Final)
                                            if (up != null) {
                                                viewModel.dismissToolbar()
                                            }
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                        ) {
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
                                    Column {
                                        ChatSessionTopBar(
                                            viewModel = viewModel,
                                            uiState = uiState,
                                            onBack = onBack,
                                            onNavigateToInfo = onNavigateToInfo
                                        )
                                        if (liveLocationRoom.isActive) {
                                            LiveLocationPinnedEntry(
                                                text = when {
                                                    liveLocationRoom.participants.size > 1 ->
                                                        stringResource(
                                                            R.string.live_location_people,
                                                            liveLocationRoom.participants.size
                                                        )

                                                    liveLocationRoom.participants.containsKey(
                                                        uiState.myId
                                                    ) ->
                                                        stringResource(R.string.live_location_me_sharing)

                                                    else -> stringResource(
                                                        R.string.live_location_peer_sharing,
                                                        uiState.title
                                                    )
                                                },
                                                avatar = if (
                                                    liveLocationRoom.participants.containsKey(
                                                        uiState.myId
                                                    )
                                                ) uiState.myAvatar else uiState.peerAvatar,
                                                onClick = onNavigateToLiveLocation
                                            )
                                        }
                                    }
                                },
                                bottomBar = {
                                    if (!uiState.isSelectMode) {
                                        InputBar(
                                            viewModel,
                                            uiState,
                                            listState,
                                            launchCall,
                                            onStartLive = {
                                                val liveId = randomUUID()
                                                viewModel.sendMessage(
                                                    MessageContent.Live(
                                                        liveId = liveId,
                                                        title = "${uiState.title}的直播",
                                                        hostName = "我",
                                                        actorId = uiState.myId
                                                    )
                                                )
                                                onNavigateToLive(liveId, true, uiState.myId.orEmpty())
                                            },
                                            onShareLiveLocation = {
                                                viewModel.sendMessage(viewModel.createLiveLocationMessage())
                                                onNavigateToLiveLocation()
                                            },
                                            onOpenFavorites = onNavigateToFavorites
                                        )
                                    } else {
                                        MultiSelectBottomBar(
                                            enabled = uiState.selectedCount > 0,
                                            onActionClick = viewModel::handleMultiSelectAction,
                                            onExitSelectMode = viewModel::exitSelectMode
                                        )
                                    }
                                },
                                containerColor =
                                    if (uiState.backgroundPath == null) {
                                        WeTheme.colorScheme.background
                                    } else {
                                        Color.Unspecified
                                    }
                            ) { innerPadding ->
                                ChatMessageList(
                                    lazyMessageItems,
                                    streamingAiMessage,
                                    uiState,
                                    toolbarState,
                                    viewModel,
                                    listState,
                                    innerPadding,
                                    bombProgress
                                )
                            }

                            MessageToolbar(
                                visible = toolbarState.visible,
                                temporarilyHidden = toolbarState.isTextSelectionDragging,
                                actions = toolbarState.actions,
                                bubblePosition = toolbarState.bubblePosition,
                                bubbleHeight = toolbarState.bubbleHeight,
                                isTextMessage =
                                    toolbarState.message?.content is MessageContent.Text,
                                onActionClick = viewModel::handleToolbarAction,
                                onDismiss = viewModel::dismissToolbar
                            )

                            BombMessageEffect(
                                trigger = bombTrigger,
                                onProgress = { bombProgress = it },
                                modifier = Modifier.fillMaxSize()
                            )
                            FestiveMessageEffect(
                                event = festiveEvent,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        BackHandler { closeMediaPreview() }
                        WeMediaPreview(
                            medias = preview.medias,
                            current = preview.initialIndex,
                            interactiveContentEnabled = !mediaPreviewClosing,
                            interactiveContentDelayMillis = 320L,
                            pageModifier = { index ->
                                Modifier.mediaSharedElement(preview.messageIds[index])
                            },
                            onDismiss = closeMediaPreview
                        )
                    }
                }
            }
        }
    }

    editingImageUri?.let { sourceUri ->
        ImageEditor(
            sourceUri = sourceUri,
            onCancel = { editingImageUri = null },
            onConfirm = { resultUri ->
                editingImageUri = null
                editedImageUri = resultUri
                showEditedImageActions = true
            }
        )
    }

    WeActionSheet(
        visible = showEditedImageActions,
        options = listOf(
            ActionSheetItem(R.string.edited_image_send_to_friend),
            ActionSheetItem(R.string.edited_image_favorite),
            ActionSheetItem(R.string.edited_image_save)
        ),
        onCancel = { showEditedImageActions = false },
        onTap = { index ->
            val uri = editedImageUri ?: return@WeActionSheet
            when (index) {
                0 -> sendEditedImageToContacts(99)
                1 -> {
                    viewModel.favoriteEditedImage(uri)
                    editedImageUri = null
                }

                2 -> {
                    viewModel.saveEditedImage(uri)
                    editedImageUri = null
                }
            }
        }
    )

    LoadingDialog(uiState.isFullscreenLoading)
}

@Composable
private fun LiveLocationPinnedEntry(
    text: String,
    avatar: Any?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4B9B72))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatar,
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .clip(androidx.compose.foundation.shape.CircleShape),
            contentScale = ContentScale.Crop
        )
        Text(
            text = text,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            color = Color.White,
            fontSize = 14.sp
        )
        Text("›", color = Color.White, fontSize = 24.sp)
    }
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
        if (viewModel.isLocalAiSession || uiState.isSelf == null || uiState.isSelf) {
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
    onNavigateToLiveLocation: () -> Unit,
    onPreviewMedia: (ChatMediaPreviewState) -> Unit,
    onEditImage: (Uri) -> Unit,
    onOpenChatHistory: (MessageContent.ChatHistory) -> Unit,
) {
    val resources = LocalResources.current
    val dialog = rememberDialogState()
    var useMergedForward by remember { mutableStateOf(false) }
    var showForwardTypeDialog by remember { mutableStateOf(false) }
    var singleForwardMessageId by remember { mutableStateOf<String?>(null) }
    val pickContact = LocalContactPickerLauncher.current.rememberLauncher { contacts ->
        dialog.show(resources.getString(R.string.msg_confirm_forward, contacts.size)) {
            val ids = contacts.map { it.id }.toSet()
            val messageId = singleForwardMessageId
            if (messageId != null) viewModel.forwardMessage(messageId, ids)
            else if (useMergedForward) viewModel.forwardMergedMessages(ids)
            else viewModel.forwardMessages(ids)
            singleForwardMessageId = null
        }
    }

    WeActionSheet(
        visible = showForwardTypeDialog,
        options = listOf(
            ActionSheetItem(R.string.message_forward_separate),
            ActionSheetItem(R.string.message_forward_merged)
        ),
        onCancel = { showForwardTypeDialog = false },
        onTap = { index ->
            showForwardTypeDialog = false
            useMergedForward = index == 1
            pickContact(99)
        }
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is MessageUiEvent.ShowDeleteConfirm -> dialog.show(
                    title = resources.getString(R.string.msg_confirm_delete),
                    okText = R.string.action_delete,
                    okColor = SemanticError
                ) {
                    if (event.messageId != null) viewModel.deleteMessage(event.messageId)
                    else viewModel.deleteSelectedMessages()
                }

                is MessageUiEvent.ShowDownloadConfirm -> dialog.show(
                    title = resources.getString(R.string.msg_confirm_save),
                    okText = R.string.action_save
                ) { viewModel.saveSelectedMessageFiles() }

                is MessageUiEvent.ForwardMessage -> {
                    singleForwardMessageId = event.messageId
                    if (event.messageId == null && viewModel.uiState.value.selectedCount > 1) {
                        showForwardTypeDialog = true
                    } else {
                        useMergedForward = false
                        pickContact(99)
                    }
                }
                is MessageUiEvent.PreviewFile -> onNavigateToFilePreview(event.messageId)
                is MessageUiEvent.PreviewMusic -> onNavigateToMusicPreview(
                    event.messageId,
                    event.trackName
                )
                is MessageUiEvent.PreviewMedia -> onPreviewMedia(
                    ChatMediaPreviewState(
                        medias = event.medias,
                        messageIds = event.messageIds,
                        initialIndex = event.initialIndex
                    )
                )

                is MessageUiEvent.EditImage -> onEditImage(event.uri)

                is MessageUiEvent.LaunchCall -> launchCall(event.callType)
                is MessageUiEvent.NavigateToContact -> onNavigateToContact(event.contactId)
                MessageUiEvent.NavigateToLiveLocation -> onNavigateToLiveLocation()
                is MessageUiEvent.OpenChatHistory -> onOpenChatHistory(event.content)
                else -> {}
            }
        }
    }
}

private data class ChatMediaPreviewState(
    val medias: List<MediaItem>,
    val messageIds: List<String>,
    val initialIndex: Int
)

/**
 * 消息列表
 */
@Composable
private fun ChatMessageList(
    lazyMessageItems: LazyPagingItems<ChatMessage>,
    streamingAiMessage: StreamingAiMessage?,
    uiState: ChatSessionUiState,
    toolbarState: MessageToolbarState,
    viewModel: ChatSessionViewModel,
    listState: LazyListState,
    innerPadding: PaddingValues,
    bombProgress: Float
) {
    val overscrollEffect = rememberBouncedOverscrollEffect()
    val scope = rememberCoroutineScope()
    val streamingMessageInPaging = streamingAiMessage?.let { streaming ->
        lazyMessageItems.itemSnapshotList.items.any { it.id == streaming.id }
    } == true

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
        if (streamingAiMessage != null && !streamingMessageInPaging) {
            item(
                key = "streaming_${streamingAiMessage.id}",
                contentType = streamingAiMessage
            ) {
                MessageItem(
                    message = streamingAiMessage.toChatMessage(uiState.peerId.orEmpty()),
                    peerAvatar = if (viewModel.isLocalAiSession) {
                        R.drawable.img_logo
                    } else uiState.peerAvatar,
                    myAvatar = uiState.myAvatar,
                    isSelectMode = false,
                    isMessageSelected = false,
                    onMessageClick = {},
                    onMessageLongPress = { _, _ -> }
                )
            }
        }

        items(
            count = lazyMessageItems.itemCount,
            key = lazyMessageItems.itemKey { it.id },
            contentType = lazyMessageItems.itemContentType { it.content.toMessageType() }
        ) { index ->
            lazyMessageItems[index]?.let { message ->
                val media = message.content as? MessageContent.Media
                val albumMessages = media?.albumId?.let { albumId ->
                    lazyMessageItems.itemSnapshotList.items.filter {
                        (it.content as? MessageContent.Media)?.albumId == albumId
                    }
                }.orEmpty()
                if (media?.albumId != null && media.albumIndex != 0) {
                    return@let
                }
                val displayMessage = if (message.id == streamingAiMessage?.id) {
                    message.copy(content = MessageContent.Text(streamingAiMessage.text))
                } else {
                    message
                }
                val phase = (displayMessage.id.hashCode() and 0xFF) / 255f * 6.28f
                val shake = bombShakeTransform(bombProgress, phase)

                MessageItem(
                    message = displayMessage,
                    albumMessages = albumMessages,
                    onAlbumMediaClick = viewModel::handleMessageClick,
                    peerAvatar = if (viewModel.isLocalAiSession) {
                        R.drawable.img_logo
                    } else uiState.peerAvatar,
                    myAvatar = uiState.myAvatar,
                    isSelectMode = uiState.isSelectMode,
                    isMessageSelected = uiState.isSelectMode && viewModel.isMessageSelected(displayMessage.id),
                    isToolbarHighlighted = toolbarState.visible &&
                            toolbarState.message?.id == displayMessage.id,
                    shakeOffsetX = shake.x,
                    shakeOffsetY = shake.y,
                    shakeRotation = shake.rotation,
                    shakeScale = shake.scale,
                    textSelection = toolbarState.textSelection.takeIf {
                        toolbarState.visible && toolbarState.message?.id == displayMessage.id
                    },
                    onTextSelectionChange = viewModel::updateTextSelection,
                    onTextSelectionDragChange = viewModel::updateTextSelectionDragging,
                    onTextSelectionBoundsChange = viewModel::updateTextSelectionBounds,
                    quoteSenderName = displayMessage.quote?.let { quote ->
                        when (quote.senderId) {
                            uiState.myId -> "我"
                            uiState.peerId -> uiState.title
                            else -> quote.senderId
                        }
                    }.orEmpty(),
                    onQuoteClick = { quotedId ->
                        val targetIndex = lazyMessageItems.itemSnapshotList.items
                            .indexOfFirst { it.id == quotedId }
                        if (targetIndex >= 0) {
                            scope.launch { listState.animateScrollToItem(targetIndex) }
                        }
                    },
                    onSwipeLeft = { viewModel.quoteMessage(displayMessage) },
                    onSwipeRight = { viewModel.forwardMessage(displayMessage) },
                    onMessageClick = {
                        if (!uiState.isSelectMode) viewModel.handleMessageClick(displayMessage)
                        else viewModel.toggleMessageSelection(displayMessage.id)
                    },
                    onMessageLongPress = { pos, height ->
                        viewModel.handleMessageLongPress(displayMessage, pos, height)
                    }
                )
                TimeDivider(lazyMessageItems.itemSnapshotList.items, index)
            }
        }

        if (lazyMessageItems.loadState.append is LoadState.Loading) {
            item(key = "load_more") {
                WeLoadMore(type = LoadMoreType.Loading)
            }
        }
    }
}

private fun StreamingAiMessage.toChatMessage(sessionId: String) = ChatMessage(
    id = id,
    sessionId = sessionId,
    senderId = LocalAiAssistant.ID,
    content = MessageContent.Text(text),
    isFromMe = false,
    timestamp = timestamp,
    sendStatus = if (isGenerating) {
        MessageSendStatus.Receiving()
    } else {
        MessageSendStatus.Delivered
    }
)

private fun ChatMessage.isBombMessage(): Boolean =
    singleSpecialEffectToken() == "[炸弹]"

private fun ChatMessage.festiveEffectType(): FestiveEffectType? {
    return when (singleSpecialEffectToken()) {
        "[烟花]" -> FestiveEffectType.Fireworks
        "[庆祝]" -> FestiveEffectType.Celebration
        "[爆竹]", "[鞭炮]" -> FestiveEffectType.Firecrackers
        else -> null
    }
}

/**
 * 微信式特效只响应单个特效表情。同一条消息包含多个特效标记时整体静默，
 * 避免多个全屏动画竞争或连续轰炸。
 */
private fun ChatMessage.singleSpecialEffectToken(): String? {
    val text = (content as? MessageContent.Text)?.text ?: return null
    var found: String? = null
    var count = 0
    SPECIAL_EFFECT_TOKENS.forEach { token ->
        var start = 0
        while (true) {
            val index = text.indexOf(token, start)
            if (index < 0) break
            count++
            if (count > 1) return null
            found = token
            start = index + token.length
        }
    }
    return found
}

private val SPECIAL_EFFECT_TOKENS = listOf(
    "[炸弹]",
    "[烟花]",
    "[庆祝]",
    "[爆竹]",
    "[鞭炮]"
)
