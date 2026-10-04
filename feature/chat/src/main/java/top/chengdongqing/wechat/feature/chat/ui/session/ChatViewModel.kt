package top.chengdongqing.wechat.feature.chat.ui.session

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.data.model.MessageQuote
import top.chengdongqing.wechat.core.data.repository.AddFriendRepository
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ChatSettingsRepository
import top.chengdongqing.wechat.core.data.repository.ConnectionSettingsRepository
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.MessageRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.FavoriteDao
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.database.entity.FavoriteEntity
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.file.PublicFileManager
import top.chengdongqing.wechat.core.file.getFileMetadata
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.location.preview.previewLocation
import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.core.model.ChatSession
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.model.UserProfile
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.connection.bluetooth.BluetoothBondManager
import top.chengdongqing.wechat.core.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.core.network.session.ActiveSessionManager
import top.chengdongqing.wechat.core.playback.SoundTipPlayer
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.feature.chat.ai.LocalAiEngine
import top.chengdongqing.wechat.feature.chat.ai.LocalAiError
import top.chengdongqing.wechat.feature.chat.ai.LocalAiState
import top.chengdongqing.wechat.feature.chat.ai.getLocalAiErrorMessage
import top.chengdongqing.wechat.feature.chat.data.mapper.getLocalPath
import top.chengdongqing.wechat.feature.chat.data.mapper.toMediaItem
import top.chengdongqing.wechat.feature.chat.data.mapper.toMessageType
import top.chengdongqing.wechat.feature.chat.data.store.AddStickerResult
import top.chengdongqing.wechat.feature.chat.data.store.StickerStore
import top.chengdongqing.wechat.feature.chat.ui.location.LiveLocationRoomState
import top.chengdongqing.wechat.feature.chat.ui.location.LiveLocationSessionRegistry
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageAction
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.feature.chat.ui.session.message.MultiMessageAction
import top.chengdongqing.wechat.feature.chat.ui.session.message.toolbar.MessageToolbarManager
import top.chengdongqing.wechat.feature.chat.ui.session.util.AudioPlaybackManager
import top.chengdongqing.wechat.feature.chat.ui.session.util.VoicePlaybackState
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.core.playback.R as PlaybackR

@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val profileRepository: ProfileRepository,
    private val chatSettingsRepository: ChatSettingsRepository,
    private val contactRepository: ContactRepository,
    groupDao: GroupDao,
    private val favoriteDao: FavoriteDao,
    private val addFriendRepository: AddFriendRepository,
    private val publicFileManager: PublicFileManager,
    private val privateFileManager: PrivateFileManager,
    private val soundTipPlayer: SoundTipPlayer,
    private val chatTransportManager: ChatTransportManager,
    private val bluetoothBondManager: BluetoothBondManager,
    private val activeSessionManager: ActiveSessionManager,
    private val localAiEngine: LocalAiEngine,
    private val liveLocationRegistry: LiveLocationSessionRegistry,
    private val stickerStore: StickerStore,
    e2eSessionManager: E2ESessionManager,
    connectionSettingsRepository: ConnectionSettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val localAiAssistantName: String
        get() = context.getString(DesignR.string.local_ai_assistant_name)

    private var aiGenerationJob: Job? = null
    private val _pendingQuote = MutableStateFlow<MessageQuote?>(null)
    private val isLocalAiSession: Boolean get() = chatId == LocalAiAssistant.ID
    private val isGroupSession: Boolean get() = chatId.startsWith("group_")
    private val _streamingAiMessage = MutableStateFlow<StreamingAiMessage?>(null)
    private val liveLocationRoom = liveLocationRegistry.rooms.map {
        it[liveLocationRegistry.roomIdFor(chatId)]
            ?: LiveLocationRoomState(liveLocationRegistry.roomIdFor(chatId))
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        liveLocationRegistry.room(liveLocationRegistry.roomIdFor(chatId))
    )

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatViewModel
    }

    fun onIntent(intent: ChatUiIntent) {
        when (intent) {
            ChatUiIntent.OnEnter -> onEnterSession()
            ChatUiIntent.OnLeave -> {
                onLeaveSession()
                stopVoice()
            }

            is ChatUiIntent.HandleToolbarAction -> handleToolbarAction(intent.action)
            is ChatUiIntent.HandleMultiSelectAction -> handleMultiSelectAction(intent.action)
            is ChatUiIntent.SendMessage -> sendMessage(intent.content)
            is ChatUiIntent.RetrySend -> retrySend(intent.messageId)
            is ChatUiIntent.SaveDraft -> saveDraftMessage(intent.text)
            is ChatUiIntent.ToggleVoicePlay -> toggleVoicePlay(
                intent.messageId,
                intent.localPath,
                intent.durationMs
            )

            is ChatUiIntent.SeekVoice -> seekVoice(intent.messageId, intent.fraction)
            is ChatUiIntent.ToggleVoiceSpeed -> toggleVoiceSpeed(intent.messageId)
            ChatUiIntent.ToggleSpeaker -> toggleSpeaker()
            is ChatUiIntent.SelectMessage -> toggleMessageSelection(intent.messageId)
            ChatUiIntent.ExitSelectMode -> exitSelectMode()
            ChatUiIntent.ClearUnread -> clearUnreadState()
            is ChatUiIntent.MessagesUpdated -> syncMessages(intent.messages)
            is ChatUiIntent.FinishAiStreamHandoff -> finishAiStreamHandoff(intent.messageId)
            ChatUiIntent.DismissToolbar -> dismissToolbar()
            is ChatUiIntent.UpdateTextSelection -> updateTextSelection(intent.selection)
            is ChatUiIntent.UpdateTextSelectionDragging -> updateTextSelectionDragging(intent.isDragging)
            is ChatUiIntent.UpdateTextSelectionBounds -> updateTextSelectionBounds(
                intent.position,
                intent.height
            )

            is ChatUiIntent.QuoteMessage -> quoteMessage(intent.message)
            is ChatUiIntent.ForwardMessage -> forwardMessage(intent.message)
            is ChatUiIntent.MessageClicked -> handleMessageClick(intent.message)
            is ChatUiIntent.MessageLongPressed -> handleMessageLongPress(
                intent.message,
                intent.position,
                intent.height
            )

            is ChatUiIntent.SendEditedImage -> sendEditedImage(intent.uri, intent.targetChatIds)
            is ChatUiIntent.FavoriteEditedImage -> favoriteEditedImage(intent.uri)
            is ChatUiIntent.SaveEditedImage -> saveEditedImage(intent.uri)
            ChatUiIntent.CancelQuote -> cancelQuote()
            ChatUiIntent.StopVoice -> stopVoice()
            is ChatUiIntent.CancelTransfer -> cancelTransfer(intent.messageId)
            is ChatUiIntent.PauseTransfer -> pauseTransfer(intent.messageId)
            is ChatUiIntent.ResumeTransfer -> resumeTransfer(intent.messageId)
            is ChatUiIntent.ReeditMessage -> reeditMessage(intent.text)
            ChatUiIntent.ShareLiveLocation -> {
                sendMessage(createLiveLocationMessage())
                emit(MessageUiEvent.NavigateToLiveLocation)
            }

            is ChatUiIntent.DeleteMessage -> if (intent.messageId != null) deleteMessage(intent.messageId) else deleteSelectedMessages()
            ChatUiIntent.SaveSelectedFiles -> saveSelectedMessageFiles()
            is ChatUiIntent.ForwardMessages -> when {
                intent.messageId != null -> forwardMessage(intent.messageId, intent.targetChatIds)
                intent.merged -> forwardMergedMessages(intent.targetChatIds)
                else -> forwardMessages(intent.targetChatIds)
            }

            ChatUiIntent.RequestAddFriend -> viewModelScope.launch {
                prepareRequestAddFriend().onSuccess {
                    emit(MessageUiEvent.NavigateToRequestAddFriend)
                }
            }

            ChatUiIntent.StartLive -> {
                val liveId = randomUUID()
                sendMessage(
                    MessageContent.Live(
                        liveId = liveId,
                        title = "${uiState.value.chatTitle.orEmpty()}的直播",
                        hostName = "我",
                        actorId = uiState.value.myUserInfo?.id
                    )
                )
                emit(
                    MessageUiEvent.NavigateToLiveRoom(
                        liveId = liveId,
                        isHost = true,
                        hostId = uiState.value.myUserInfo?.id.orEmpty()
                    )
                )
            }
        }
    }

    private val messagePagingFlow: Flow<PagingData<ChatMessage>> = messageRepository
        .pager(
            sessionId = chatId,
            pageSize = 10,
            prefetchDistance = 1
        )
        .cachedIn(viewModelScope)

    private val interactionState = MutableStateFlow(
        ChatUiState(
            chatId = chatId,
            chatType = when {
                chatId == LocalAiAssistant.ID -> ChatType.Ai
                chatId.startsWith("group_") -> ChatType.Group
                else -> ChatType.Single
            }
        )
    )

    private val chatIdentity = combine(
        contactRepository.observeContact(chatId),
        profileRepository.observeProfile()
    ) { contact, profile ->
        ChatIdentity(
            peerUserInfo = contact,
            myUserInfo = profile,
            isInfoLoaded = true,
            chatType = when {
                chatId == LocalAiAssistant.ID -> ChatType.Ai
                chatId.startsWith("group_") -> ChatType.Group
                chatId == profile?.id -> ChatType.Self
                else -> ChatType.Single
            }
        )
    }

    private val groupPresentation = if (isGroupSession) {
        groupDao.observeById(chatId)
            .combine(groupDao.observeMembers(chatId)) { group, members ->
                GroupPresentation(
                    title = group?.remark?.takeIf(String::isNotBlank)
                        ?: group?.name.orEmpty(),
                    members = members.map { MentionMember(it.userId, it.nickname, it.avatarPath) }
                )
            }
    } else {
        flowOf(null)
    }

    private val sessionPresentation = chatSessionRepository.observeSession(chatId)
        .combine(chatSettingsRepository.chatBackground) { session, globalBackground ->
            session to globalBackground
        }
        .combine(localAiEngine.state) { (session, globalBackground), aiState ->
            ChatSessionPresentation(
                title = session?.contactName,
                peerId = session?.contactId,
                peerAvatar = session?.contactAvatar,
                isMuted = session?.isMuted ?: false,
                isTemporary = session?.isTemporary == true,
                isOnline = if (isLocalAiSession) aiState is LocalAiState.Ready
                else session?.isOnline ?: false,
                draftMessage = session?.draftMessage,
                backgroundPath = session?.backgroundPath ?: globalBackground
            )
        }

    private val settingsPresentation = combine(
        chatSettingsRepository.speakerEnabled,
        chatSettingsRepository.sendButtonEnabled
    ) { speakerEnabled, sendButtonEnabled ->
        ChatSettingsPresentation(speakerEnabled, sendButtonEnabled)
    }

    private val connectionRequired = chatTransportManager.connectionRequired
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val connectionMode = connectionSettingsRepository.connectionMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionMode.WiFiLan)

    private val isE2EActive = e2eSessionManager.encryptedPeers
        .map { it.contains(chatId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val unreadCount = chatSessionRepository.observeTotalUnreadCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val connectionPresentation = combine(
        connectionMode,
        connectionRequired,
        isE2EActive,
        unreadCount
    ) { mode, required, isEncrypted, unread ->
        ChatConnectionPresentation(mode, required, isEncrypted, unread)
    }

    private val voicePlaybackState = MutableStateFlow(VoicePlaybackState())

    private val basePresentation = combine(
        chatIdentity,
        groupPresentation,
        sessionPresentation
    ) { identity, group, session ->
        ChatBasePresentation(
            chatTitle = when {
                isLocalAiSession -> localAiAssistantName
                group != null -> group.title
                !session.title.isNullOrBlank() -> session.title
                !identity.peerUserInfo?.displayName.isNullOrBlank() -> identity.peerUserInfo.displayName
                identity.chatType == ChatType.Self -> identity.myUserInfo?.nickname.orEmpty()
                else -> ""
            },
            peerUserInfo = identity.peerUserInfo,
            myUserInfo = identity.myUserInfo,
            isInfoLoaded = identity.isInfoLoaded,
            chatType = identity.chatType,
            mentionMembers = group?.members.orEmpty()
                .filterNot { it.id == identity.myUserInfo?.id },
            isMuted = session.isMuted,
            isTemporary = session.isTemporary,
            isOnline = session.isOnline,
            draftMessage = session.draftMessage,
            backgroundImagePath = session.backgroundPath
        )
    }

    private val _uiEvent = MutableSharedFlow<MessageUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private fun emit(event: MessageUiEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }

    private val messages = MutableStateFlow(emptyList<ChatMessage>())

    private fun syncMessages(list: List<ChatMessage>) {
        messages.update { list }
    }

    /**
     * 媒体预览索引表
     *
     * 仅在消息 ID 或媒体路径真正变化时才在后台线程重建索引，避免无效计算。
     */
    private val mediaState = messages
        .map { list ->
            list.map { msg ->
                Triple(
                    msg.id,
                    msg.content is MessageContent.Media,
                    (msg.content as? MessageContent.Media)?.localPath
                )
            }
        }
        .distinctUntilChanged()
        .map { withContext(Dispatchers.Default) { buildMediaState(messages.value) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MediaState())

    private fun buildMediaState(messages: List<ChatMessage>): MediaState {
        val items = mutableListOf<MediaItem>()
        val messageIds = mutableListOf<String>()
        val indexMap = mutableMapOf<String, Int>()
        for (i in messages.indices.reversed()) {
            (messages[i].content as? MessageContent.Media)?.toMediaItem()?.let {
                items.add(it)
                messageIds.add(messages[i].id)
                indexMap[messages[i].id] = items.lastIndex
            }
        }
        return MediaState(list = items, messageIds = messageIds, indexMap = indexMap)
    }

    private fun isConnected(): Boolean = chatTransportManager.isConnected(chatId)
    private suspend fun isBluetoothDeviceSaved() = bluetoothBondManager.hasSaved(chatId)

    private val toolbarManager = MessageToolbarManager(
        context = context,
        scope = viewModelScope,
        uiEvent = _uiEvent,
        onRecallMessage = ::recallMessage,
        onCancelMessage = ::cancelTransfer,
        onToggleSpeaker = ::toggleSpeaker,
        onSaveFile = ::saveFile,
        onMultiSelect = ::enterSelectMode,
        onQuote = ::quoteMessage,
        onAddSticker = ::addSticker
    )

    private val toolbarState = toolbarManager.state

    private val interactionPresentation = combine(
        interactionState,
        _pendingQuote,
        _streamingAiMessage,
        toolbarState,
        liveLocationRoom
    ) { interaction, quote, streaming, toolbar, liveRoom ->
        interaction.copy(
            pendingQuote = quote,
            streamingAiMessage = streaming,
            toolbarState = toolbar,
            liveLocationRoom = liveRoom
        )
    }

    val uiState: StateFlow<ChatUiState> = combine(
        basePresentation,
        settingsPresentation,
        connectionPresentation,
        voicePlaybackState,
        interactionPresentation
    ) { base, settings, connection, voice, interaction ->
        interaction.copy(
            chatTitle = base.chatTitle,
            peerUserInfo = base.peerUserInfo,
            myUserInfo = base.myUserInfo,
            isInfoLoaded = base.isInfoLoaded,
            chatType = base.chatType,
            mentionMembers = base.mentionMembers,
            isMuted = base.isMuted,
            isTemporary = base.isTemporary,
            isOnline = base.isOnline,
            draftMessage = base.draftMessage,
            backgroundImagePath = base.backgroundImagePath,
            isSpeakerOn = settings.speakerEnabled,
            isSendButtonOn = settings.sendButtonEnabled,
            isE2EActive = connection.isE2EActive,
            totalUnreadCount = connection.totalUnreadCount,
            connectionMode = connection.mode,
            connectionRequired = connection.required,
            isConnected = ::isConnected,
            isBluetoothDeviceBonded = ::isBluetoothDeviceSaved,
            voicePlaybackState = voice,
            messagePaging = messagePagingFlow
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = interactionState.value.copy(
            messagePaging = messagePagingFlow,
            liveLocationRoom = liveLocationRegistry.room(liveLocationRegistry.roomIdFor(chatId))
        )
    )

    private fun createLiveLocationMessage() = MessageContent.LiveLocation(
        roomId = liveLocationRegistry.roomIdFor(chatId),
        initiatorId = profileRepository.requireUserId()
    )

    private fun onEnterSession() {
        activeSessionManager.enter(chatId)
        clearUnreadState()
    }

    private fun onLeaveSession() = activeSessionManager.leave()

    private fun handleMessageLongPress(
        message: ChatMessage,
        bubblePosition: Offset,
        bubbleHeight: Float
    ) {
        toolbarManager.onLongPress(
            message = message,
            bubblePosition = bubblePosition,
            bubbleHeight = bubbleHeight,
            isSpeakerOn = uiState.value.isSpeakerOn
        )
    }

    private fun handleToolbarAction(action: MessageAction) {
        if (action == MessageAction.Favorite) {
            toolbarManager.state.value.message?.let { favoriteMessages(listOf(it)) }
        }
        toolbarManager.onAction(action)
    }

    private fun dismissToolbar() = toolbarManager.dismiss()

    private fun quoteMessage(message: ChatMessage) {
        _pendingQuote.value = MessageQuote(
            messageId = message.id,
            senderId = message.senderId,
            messageType = message.content.toMessageType(),
            preview = message.content.quotePreview()
        )
    }

    private fun forwardMessage(message: ChatMessage) {
        if (!message.content.toMessageType().isForwardable) return
        emit(MessageUiEvent.ForwardMessage(message.id))
    }

    private fun forwardMessage(messageId: String, targetChatIds: Set<String>) {
        viewModelScope.launch {
            messageRepository.forwardMessages(setOf(messageId), targetChatIds)
            context.showToast("已发送")
        }
    }

    private fun cancelQuote() {
        _pendingQuote.value = null
    }

    private fun addSticker(message: ChatMessage) {
        val path = (message.content as? MessageContent.Sticker)?.localPath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (!File(path).isFile) {
                withContext(Dispatchers.Main) { context.showToast("表情文件不存在") }
                return@launch
            }
            val result = stickerStore.add(path)
            withContext(Dispatchers.Main) {
                context.showToast(
                    if (result == AddStickerResult.Added) "已添加到表情"
                    else "该表情已添加"
                )
            }
        }
    }

    private fun updateTextSelection(selection: TextRange) {
        toolbarManager.updateTextSelection(selection)
    }

    private fun updateTextSelectionDragging(isDragging: Boolean) {
        toolbarManager.updateTextSelectionDragging(isDragging)
    }

    private fun updateTextSelectionBounds(position: Offset, height: Float) {
        toolbarManager.updateTextSelectionBounds(position, height)
    }

    private val audioPlaybackManager = AudioPlaybackManager(
        context = context,
        scope = viewModelScope,
        soundTipPlayer = soundTipPlayer,
        onPlaybackStateChanged = {
            voicePlaybackState.value = it
        },
        onMessagePlayed = ::markAsPlayed
    )

    private fun toggleVoicePlay(messageId: String, localPath: String, durationMs: Long) {
        audioPlaybackManager.togglePlay(
            messageId = messageId,
            localPath = localPath,
            expectedDurationMs = durationMs,
            messages = messages.value.filter { it.content is MessageContent.Voice },
            isSpeakerOn = uiState.value.isSpeakerOn
        )
    }

    private fun stopVoice() {
        if (voicePlaybackState.value.messageId != null) audioPlaybackManager.stop()
    }

    private fun seekVoice(messageId: String, fraction: Float) {
        audioPlaybackManager.seekTo(messageId, fraction)
    }

    private fun toggleVoiceSpeed(messageId: String) {
        audioPlaybackManager.toggleSpeed(messageId)
    }

    private fun toggleSpeaker() {
        val isSpeakerOn = !uiState.value.isSpeakerOn
        audioPlaybackManager.setSpeakerOn(isSpeakerOn)
        viewModelScope.launch {
            chatSettingsRepository.toggleSpeaker(isSpeakerOn)
        }
    }

    private fun markAsPlayed(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messages.value.find { it.id == messageId }
                ?.takeIf { it.content.showUnreadDot }
                ?.let { messageRepository.markVoiceAsPlayed(messageId) }
        }
    }

    private fun clearUnreadState() {
        viewModelScope.launch { messageRepository.markAllAsRead(chatId) }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(chatId.hashCode())
    }

    private fun sendMessage(content: MessageContent) {
        viewModelScope.launch {
            if (chatId == LocalAiAssistant.ID && !chatSessionRepository.exists(chatId)) {
                chatSessionRepository.createSession(
                    ChatSession(
                        id = chatId,
                        contactId = chatId,
                        contactName = localAiAssistantName
                    )
                )
            }
            messageRepository.sendMessage(
                sessionId = chatId,
                receiverId = chatId,
                content = content,
                quote = _pendingQuote.value
            ).onSuccess {
                _pendingQuote.value = null
                if (content is MessageContent.Voice) {
                    soundTipPlayer.play(PlaybackR.raw.tip_after_upload_voice)
                }
                if (chatId == LocalAiAssistant.ID && content is MessageContent.Text) {
                    generateAiReply(content.text)
                }
            }
        }
    }

    private fun MessageContent.quotePreview(): String = when (this) {
        is MessageContent.Text -> text.replace('\n', ' ').trim().take(160)
        is MessageContent.Voice -> "[语音]"
        is MessageContent.Sticker -> "[表情]"
        is MessageContent.Image -> "[图片]"
        is MessageContent.Video -> "[视频]"
        is MessageContent.Media -> "[媒体]"
        is MessageContent.Call -> "[通话]"
        is MessageContent.Location -> "[位置] ${poiName.ifBlank { address }}"
        is MessageContent.LiveLocation -> "[位置共享]"
        is MessageContent.File -> "[文件] $filename"
        is MessageContent.ContactCard -> "[名片] $nickname"
        is MessageContent.Music -> "[音乐] ${music.title}"
        is MessageContent.Live -> "[直播] $title"
        is MessageContent.ChatHistory -> "[聊天记录] $title"
    }

    private fun generateAiReply(prompt: String) {
        aiGenerationJob?.cancel()
        aiGenerationJob = viewModelScope.launch {
            val receiverId = uiState.value.myUserInfo?.id ?: return@launch
            val messageId = randomUUID()
            val timestamp = System.currentTimeMillis()
            val response = StringBuffer()
            _streamingAiMessage.value = StreamingAiMessage(
                id = messageId,
                text = "",
                timestamp = timestamp,
                isGenerating = true
            )
            val persistenceSignals = Channel<Boolean>(Channel.CONFLATED)
            val persistenceJob = launch(Dispatchers.IO) {
                var isFirstWrite = true
                for (isFinal in persistenceSignals) {
                    messageRepository.upsertLocalAssistantMessage(
                        sessionId = chatId,
                        messageId = messageId,
                        senderId = LocalAiAssistant.ID,
                        receiverId = receiverId,
                        text = response.toString(),
                        updateSessionPreview = isFirstWrite || isFinal
                    )
                    isFirstWrite = false
                    if (!isFinal) {
                        delay(AI_STREAM_PERSIST_INTERVAL_MS.milliseconds)
                    }
                }
            }

            try {
                localAiEngine.generate(prompt).collect { token ->
                    response.append(token)
                    _streamingAiMessage.update { current ->
                        current?.takeIf { it.id == messageId }?.copy(text = response.toString())
                            ?: current
                    }
                    persistenceSignals.trySend(false)
                }
                persistenceSignals.send(true)
            } catch (error: CancellationException) {
                withContext(NonCancellable) {
                    persistenceSignals.close()
                    persistenceJob.cancelAndJoin()
                    if (response.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            messageRepository.upsertLocalAssistantMessage(
                                sessionId = chatId,
                                messageId = messageId,
                                senderId = LocalAiAssistant.ID,
                                receiverId = receiverId,
                                text = response.toString(),
                                updateSessionPreview = true
                            )
                        }
                    }
                    markAiStreamCompleted(messageId, response.toString())
                }
                throw error
            } catch (error: Throwable) {
                val text = context.getLocalAiErrorMessage(error, LocalAiError.INFERENCE_FAILED)
                if (response.isEmpty()) {
                    response.append(text)
                } else {
                    response.append("\n\n")
                        .append(context.getString(R.string.local_ai_generation_interrupted, text))
                }
                _streamingAiMessage.update { current ->
                    current?.takeIf { it.id == messageId }?.copy(text = response.toString())
                        ?: current
                }
                persistenceSignals.send(true)
            } finally {
                persistenceSignals.close()
                if (!persistenceJob.isCancelled) {
                    persistenceJob.join()
                }
            }
            markAiStreamCompleted(messageId, response.toString())
        }
    }

    private fun markAiStreamCompleted(messageId: String, text: String) {
        _streamingAiMessage.update { current ->
            current?.takeIf { it.id == messageId }?.copy(
                text = text,
                isGenerating = false
            ) ?: current
        }
    }

    private fun finishAiStreamHandoff(messageId: String) {
        _streamingAiMessage.update { current ->
            if (current?.id == messageId && !current.isGenerating) null else current
        }
    }

    private fun retrySend(messageId: String) {
        viewModelScope.launch { messageRepository.retrySend(messageId) }
    }

    private fun saveDraftMessage(draft: String) {
        viewModelScope.launch {
            chatSessionRepository.updateDraft(
                sessionId = chatId,
                draft = draft.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch { messageRepository.deleteMessage(messageId) }
    }

    private fun recallMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.recallMessage(messageId).onFailure {
                context.showToast(
                    it.message ?: context.getString(DesignR.string.msg_process_failed)
                )
            }
        }
    }

    private fun saveFile(message: ChatMessage) {
        val content = message.content
        val file = File(content.getLocalPath() ?: return)
        val filename = if (content is MessageContent.File) content.filename else null
        viewModelScope.launch {
            val saved = publicFileManager.saveMedia(
                messageType = content.toMessageType(),
                sourceFile = file,
                filename = filename
            )
            context.showToast(if (saved != null) "已保存到本地" else "保存失败")
        }
    }

    private fun sendEditedImage(uri: Uri, targetChatIds: Set<String>) {
        if (targetChatIds.isEmpty()) return
        viewModelScope.launch {
            val content = persistEditedImage(uri) ?: return@launch
            val results = targetChatIds.map { targetId ->
                async {
                    messageRepository.sendMessage(
                        sessionId = targetId,
                        receiverId = targetId,
                        content = content
                    )
                }
            }.awaitAll()
            context.showToast(if (results.all { it.isSuccess }) "已发送" else "部分发送失败")
        }
    }

    private fun favoriteEditedImage(uri: Uri) {
        viewModelScope.launch {
            val content = persistEditedImage(uri) ?: return@launch
            val now = System.currentTimeMillis()
            favoriteDao.upsert(
                FavoriteEntity(
                    id = randomUUID(),
                    type = "MEDIA",
                    title = "[图片]",
                    content = "[图片]",
                    mediaPaths = content.localPath,
                    sourceMessageIds = "",
                    sourceName = uiState.value.chatTitle.orEmpty(),
                    createdAt = now,
                    updatedAt = now
                )
            )
            context.showToast("已收藏")
        }
    }

    private fun saveEditedImage(uri: Uri) {
        viewModelScope.launch {
            val saved = publicFileManager.saveMedia(MessageType.Image, uri)
            context.showToast(if (saved != null) "已保存到本地" else "保存失败")
        }
    }

    private suspend fun persistEditedImage(uri: Uri): MessageContent.Image? {
        val metadata = context.getFileMetadata(uri) ?: run {
            context.showToast("图片处理失败")
            return null
        }
        val localPath = privateFileManager.saveMedia(MessageType.Image, uri).getOrElse {
            context.showToast("图片处理失败")
            return null
        }
        return MessageContent.Image(
            localPath = localPath,
            filename = metadata.filename,
            mimeType = metadata.mimeType,
            width = metadata.width,
            height = metadata.height,
            size = metadata.size
        )
    }

    private fun pauseTransfer(messageId: String) {
        viewModelScope.launch {
            messageRepository.pauseTransfer(messageId).onFailure {
                context.showToast(context.getString(DesignR.string.msg_process_failed))
            }
        }
    }

    private fun resumeTransfer(messageId: String) {
        viewModelScope.launch {
            messageRepository.resumeTransfer(messageId).onFailure {
                context.showToast(context.getString(DesignR.string.msg_process_failed))
            }
        }
    }

    private fun cancelTransfer(messageId: String) {
        viewModelScope.launch { messageRepository.cancelTransfer(messageId) }
    }

    private fun reeditMessage(text: String) = emit(MessageUiEvent.ReeditMessage(text))

    private fun handleMessageClick(message: ChatMessage) {
        when (val content = message.content) {
            is MessageContent.Image,
            is MessageContent.Video -> if (content.localPath.isNotBlank()) openMediaPreview(message)

            is MessageContent.Voice -> toggleVoicePlay(
                message.id,
                content.localPath,
                content.duration
            )

            is MessageContent.File -> emit(MessageUiEvent.PreviewFile(message.id))
            is MessageContent.Music -> emit(
                MessageUiEvent.PreviewMusic(message.id, Json.encodeToString(content.music))
            )

            is MessageContent.Call -> emit(MessageUiEvent.LaunchCall(content.type))
            is MessageContent.Location -> openLocationPreview(content)
            is MessageContent.LiveLocation -> {
                if (liveLocationRegistry.room(content.roomId).isActive) {
                    emit(MessageUiEvent.NavigateToLiveLocation)
                }
            }

            is MessageContent.ContactCard -> viewModelScope.launch {
                val userId = content.userId
                prepareRequestAddFriend(userId = userId, fromContactCard = true)
                    .onSuccess { _uiEvent.emit(MessageUiEvent.NavigateToContact(userId)) }
            }

            is MessageContent.ChatHistory -> emit(MessageUiEvent.OpenChatHistory(content))

            else -> {}
        }
    }

    private fun openMediaPreview(message: ChatMessage) {
        val index = mediaState.value.indexMap[message.id] ?: run {
            Log.e("MediaPreview", "找不到该消息的媒体索引: ${message.id}")
            return
        }
        emit(
            MessageUiEvent.PreviewMedia(
                medias = mediaState.value.list,
                messageIds = mediaState.value.messageIds,
                initialIndex = index
            )
        )
    }

    private fun openLocationPreview(content: MessageContent.Location) {
        context.previewLocation(
            LocationPreviewInfo(
                coordinate = GeoPoint(content.latitude, content.longitude),
                address = content.address,
                name = content.poiName
            )
        )
    }

    private fun enterSelectMode(messageId: String) {
        interactionState.update {
            it.copy(
                isSelectMode = true,
                selectedMessageIds = setOf(messageId)
            )
        }
    }

    private fun exitSelectMode() {
        interactionState.update { it.copy(isSelectMode = false, selectedMessageIds = emptySet()) }
    }

    private fun toggleMessageSelection(messageId: String) {
        interactionState.update { state ->
            val newIds = if (messageId in state.selectedMessageIds) {
                state.selectedMessageIds - messageId
            } else {
                state.selectedMessageIds + messageId
            }
            state.copy(selectedMessageIds = newIds)
        }
    }

    private fun deleteSelectedMessages() {
        val ids = uiState.value.selectedMessageIds
        viewModelScope.launch { messageRepository.deleteMessages(ids, chatId) }
        exitSelectMode()
    }

    private fun saveSelectedMessageFiles() {
        val ids = uiState.value.selectedMessageIds
        exitSelectMode()

        viewModelScope.launch {
            val contents = ids.mapNotNull { id ->
                messages.value.find { it.id == id }?.content?.takeIf { it.getLocalPath() != null }
            }
            if (contents.isEmpty()) {
                context.showToast("没有找到可以保存的内容")
                return@launch
            }

            interactionState.update { it.copy(isFullscreenLoading = true) }

            val results = contents.map { content ->
                val localPath = checkNotNull(content.getLocalPath())
                val filename = if (content is MessageContent.File) content.filename else null
                async {
                    publicFileManager.saveMedia(
                        messageType = content.toMessageType(),
                        sourceFile = File(localPath),
                        filename = filename
                    )
                }
            }.awaitAll()

            val successCount = results.count { it != null }
            val failCount = results.size - successCount

            interactionState.update { it.copy(isFullscreenLoading = false) }
            context.showToast(
                when {
                    failCount == 0 -> "已保存 $successCount 个文件"
                    successCount == 0 -> "保存失败"
                    else -> "已保存 $successCount 个文件，$failCount 个失败"
                }
            )
        }
    }

    private fun forwardMessages(targetChatIds: Set<String>) {
        val ids = uiState.value.selectedMessageIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            messageRepository.forwardMessages(ids, targetChatIds)
            context.showToast("已发送")
        }
        exitSelectMode()
    }

    private fun forwardMergedMessages(targetChatIds: Set<String>) {
        val ids = uiState.value.selectedMessageIds
        if (ids.isEmpty()) return
        val title = "${uiState.value.chatTitle.orEmpty()}的聊天记录"
        viewModelScope.launch {
            messageRepository.forwardMergedMessages(
                ids = ids,
                targetChatIds = targetChatIds,
                historyTitle = title,
                myName = "我",
                peerName = uiState.value.chatTitle.orEmpty()
            )
            context.showToast("已发送")
        }
        exitSelectMode()
    }

    private fun handleMultiSelectAction(action: MultiMessageAction) {
        when (action) {
            MultiMessageAction.Forward -> emit(MessageUiEvent.ForwardMessage())
            MultiMessageAction.Delete -> emit(MessageUiEvent.ShowDeleteConfirm())
            MultiMessageAction.Download -> emit(MessageUiEvent.ShowDownloadConfirm)
            MultiMessageAction.Favorite -> viewModelScope.launch {
                val messages = uiState.value.selectedMessageIds.mapNotNull {
                    messageRepository.getMessage(it)
                }
                favoriteMessages(messages)
                exitSelectMode()
            }
        }
    }

    private fun favoriteMessages(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val first = messages.first()
            val previews = messages.map { it.content.favoritePreview() }
            favoriteDao.upsert(
                FavoriteEntity(
                    id = randomUUID(),
                    type = if (messages.size > 1) "RICH_TEXT" else first.content.favoriteType(),
                    title = if (messages.size > 1) "${messages.size} 条聊天记录" else previews.first(),
                    content = previews.joinToString("\n"),
                    mediaPaths = messages.mapNotNull { it.content.getLocalPath() }
                        .joinToString("\n"),
                    sourceMessageIds = messages.joinToString(",") { it.id },
                    sourceName = uiState.value.chatTitle.orEmpty(),
                    createdAt = now,
                    updatedAt = now
                )
            )
            context.showToast("已收藏")
        }
    }

    private fun MessageContent.favoriteType(): String = when (this) {
        is MessageContent.Voice -> "VOICE"
        is MessageContent.Location -> "LOCATION"
        is MessageContent.Image, is MessageContent.Video, is MessageContent.File,
        is MessageContent.Sticker -> "MEDIA"

        else -> "RICH_TEXT"
    }

    private fun MessageContent.favoritePreview(): String = when (this) {
        is MessageContent.Text -> text
        is MessageContent.Voice -> duration.toString()
        is MessageContent.Location -> "$latitude|$longitude|$address"
        else -> quotePreview()
    }

    private suspend fun prepareRequestAddFriend(
        userId: String = chatId,
        fromContactCard: Boolean = false
    ): Result<Unit> {
        if (fromContactCard && (userId == uiState.value.myUserInfo?.id || contactRepository.exists(
                userId
            ))
        ) {
            return Result.success(Unit)
        }

        interactionState.update { it.copy(isFullscreenLoading = true) }
        return runCatching {
            if (addFriendRepository.fetchProfile(userId) == null) {
                context.showToast(context.getString(DesignR.string.add_contact_fetch_profile_failed))
                error("failed to fetch profile for $userId")
            }
        }.also {
            interactionState.update { it.copy(isFullscreenLoading = false) }
        }
    }

    override fun onCleared() {
        aiGenerationJob?.cancel()
        audioPlaybackManager.release()
    }
}

private data class MediaState(
    val list: List<MediaItem> = emptyList(),
    val messageIds: List<String> = emptyList(),
    val indexMap: Map<String, Int> = emptyMap()
)

data class StreamingAiMessage(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isGenerating: Boolean
)

private const val AI_STREAM_PERSIST_INTERVAL_MS = 400L

private data class ChatIdentity(
    val peerUserInfo: Contact?,
    val myUserInfo: UserProfile?,
    val isInfoLoaded: Boolean,
    val chatType: ChatType
)

private data class GroupPresentation(
    val title: String,
    val members: List<MentionMember>
)

private data class ChatSessionPresentation(
    val title: String?,
    val peerId: String?,
    val peerAvatar: String?,
    val isMuted: Boolean,
    val isTemporary: Boolean,
    val isOnline: Boolean,
    val draftMessage: String?,
    val backgroundPath: String?
)

private data class ChatSettingsPresentation(
    val speakerEnabled: Boolean,
    val sendButtonEnabled: Boolean
)

private data class ChatConnectionPresentation(
    val mode: ConnectionMode,
    val required: top.chengdongqing.wechat.core.network.connection.ConnectionRequiredEvent?,
    val isE2EActive: Boolean,
    val totalUnreadCount: Int
)

private data class ChatBasePresentation(
    val chatTitle: String,
    val peerUserInfo: Contact?,
    val myUserInfo: UserProfile?,
    val isInfoLoaded: Boolean,
    val chatType: ChatType,
    val mentionMembers: List<MentionMember>,
    val isMuted: Boolean,
    val isTemporary: Boolean,
    val isOnline: Boolean,
    val draftMessage: String?,
    val backgroundImagePath: String?
)
