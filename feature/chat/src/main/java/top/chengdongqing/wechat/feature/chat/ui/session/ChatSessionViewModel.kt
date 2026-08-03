package top.chengdongqing.wechat.feature.chat.ui.session

import android.app.NotificationManager
import android.content.Context
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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.common.file.PublicFileManager
import top.chengdongqing.wechat.core.common.media.SoundTipPlayer
import top.chengdongqing.wechat.core.common.media.model.MediaItem
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
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.location.preview.previewLocation
import top.chengdongqing.wechat.core.model.ChatSession
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.connection.bluetooth.BluetoothBondManager
import top.chengdongqing.wechat.core.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.core.network.session.ActiveSessionManager
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.feature.chat.ai.LocalAiEngine
import top.chengdongqing.wechat.feature.chat.ai.LocalAiState
import top.chengdongqing.wechat.feature.chat.data.mapper.getLocalPath
import top.chengdongqing.wechat.feature.chat.data.mapper.toMediaItem
import top.chengdongqing.wechat.feature.chat.data.mapper.toMessageType
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

@HiltViewModel(assistedFactory = ChatSessionViewModel.Factory::class)
class ChatSessionViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val profileRepository: ProfileRepository,
    private val chatSettingsRepository: ChatSettingsRepository,
    private val contactRepository: ContactRepository,
    private val groupDao: GroupDao,
    private val favoriteDao: FavoriteDao,
    private val addFriendRepository: AddFriendRepository,
    private val publicFileManager: PublicFileManager,
    private val soundTipPlayer: SoundTipPlayer,
    private val chatTransportManager: ChatTransportManager,
    private val bluetoothBondManager: BluetoothBondManager,
    private val activeSessionManager: ActiveSessionManager,
    private val localAiEngine: LocalAiEngine,
    private val liveLocationRegistry: LiveLocationSessionRegistry,
    e2eSessionManager: E2ESessionManager,
    connectionSettingsRepository: ConnectionSettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private var aiGenerationJob: Job? = null
    private val _pendingQuote = MutableStateFlow<MessageQuote?>(null)
    val pendingQuote = _pendingQuote.asStateFlow()
    val isLocalAiSession: Boolean get() = chatId == LocalAiAssistant.ID
    val isGroupSession: Boolean get() = chatId.startsWith("group_")
    val localAiState = localAiEngine.state
    private val _streamingAiMessage = MutableStateFlow<StreamingAiMessage?>(null)
    val streamingAiMessage = _streamingAiMessage.asStateFlow()
    val liveLocationRoom = liveLocationRegistry.rooms.map {
        it[liveLocationRegistry.roomIdFor(chatId)]
            ?: LiveLocationRoomState(liveLocationRegistry.roomIdFor(chatId))
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        liveLocationRegistry.room(liveLocationRegistry.roomIdFor(chatId))
    )

    fun createLiveLocationMessage() = MessageContent.LiveLocation(
        roomId = liveLocationRegistry.roomIdFor(chatId),
        initiatorId = profileRepository.requireUserId()
    )

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatSessionViewModel
    }

    // ── 生命周期 ──────────────────────────────────────────────────────────────

    fun onEnterSession() = activeSessionManager.enter(chatId)
    fun onLeaveSession() = activeSessionManager.leave()

    // ── 核心状态 ──────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(ChatSessionUiState())
    val uiState = _uiState.asStateFlow()

    /** UI 事件总线，供 Screen 层响应一次性操作 */
    private val _uiEvent = MutableSharedFlow<MessageUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    /** 当前正在播放语音的消息 ID */
    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId = _playingMessageId.asStateFlow()

    private val _voicePlaybackState = MutableStateFlow(VoicePlaybackState())
    val voicePlaybackState = _voicePlaybackState.asStateFlow()

    /** 在新协程中发射 UI 事件，省去调用侧的样板代码 */
    private fun emit(event: MessageUiEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }

    // region 消息流

    val messagePagingFlow: Flow<PagingData<ChatMessage>> = messageRepository
        .pager(
            sessionId = chatId,
            pageSize = 10,
            prefetchDistance = 1
        )
        .cachedIn(viewModelScope)

    private val messages = MutableStateFlow(emptyList<ChatMessage>())

    // 同步paging内部已加载的数据
    fun syncMessages(list: List<ChatMessage>) {
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

    // endregion

    // region 连接 & 加密

    val connectionRequired = chatTransportManager.connectionRequired
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val connectionMode = connectionSettingsRepository.connectionMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionMode.WiFiLan)

    val isE2EActive = e2eSessionManager.encryptedPeers
        .map { it.contains(chatId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val unreadCount = chatSessionRepository.observeTotalUnreadCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun isConnected(): Boolean = chatTransportManager.isConnected(chatId)
    suspend fun isBluetoothDeviceSaved() = bluetoothBondManager.hasSaved(chatId)

    // endregion

    // region 工具条

    private val toolbarManager = MessageToolbarManager(
        context = context,
        scope = viewModelScope,
        uiEvent = _uiEvent,
        onRecallMessage = ::recallMessage,
        onCancelMessage = ::cancelTransfer,
        onToggleSpeaker = ::toggleSpeaker,
        onSaveFile = ::saveFile,
        onMultiSelect = ::enterSelectMode,
        onQuote = ::quoteMessage
    )

    val toolbarState = toolbarManager.state

    fun handleMessageLongPress(message: ChatMessage, bubblePosition: Offset, bubbleHeight: Float) {
        toolbarManager.onLongPress(
            message = message,
            bubblePosition = bubblePosition,
            bubbleHeight = bubbleHeight,
            isSpeakerOn = _uiState.value.isSpeakerOn
        )
    }

    fun handleToolbarAction(action: MessageAction) {
        if (action == MessageAction.Forward) {
            toolbarManager.state.value.message?.id?.let { enterSelectMode(it) }
        } else if (action == MessageAction.Favorite) {
            toolbarManager.state.value.message?.let { favoriteMessages(listOf(it)) }
        }
        toolbarManager.onAction(action)
    }

    fun dismissToolbar() = toolbarManager.dismiss()

    private fun quoteMessage(message: ChatMessage) {
        _pendingQuote.value = MessageQuote(
            messageId = message.id,
            senderId = message.senderId,
            messageType = message.content.toMessageType(),
            preview = message.content.quotePreview()
        )
    }

    fun cancelQuote() {
        _pendingQuote.value = null
    }

    fun updateTextSelection(selection: TextRange) {
        toolbarManager.updateTextSelection(selection)
    }

    fun updateTextSelectionDragging(isDragging: Boolean) {
        toolbarManager.updateTextSelectionDragging(isDragging)
    }

    fun updateTextSelectionBounds(position: Offset, height: Float) {
        toolbarManager.updateTextSelectionBounds(position, height)
    }

    // endregion

    // region 语音播放

    private val audioPlaybackManager = AudioPlaybackManager(
        context = context,
        scope = viewModelScope,
        soundTipPlayer = soundTipPlayer,
        onPlaybackStateChanged = {
            _voicePlaybackState.value = it
            _playingMessageId.value = it.messageId.takeIf { _ -> it.isPlaying }
        },
        onMessagePlayed = ::markAsPlayed
    )

    fun toggleVoicePlay(messageId: String, localPath: String) {
        audioPlaybackManager.togglePlay(
            messageId = messageId,
            localPath = localPath,
            messages = messages.value.filter { it.content is MessageContent.Voice },
            isSpeakerOn = _uiState.value.isSpeakerOn
        )
    }

    fun stopVoice() {
        if (_voicePlaybackState.value.messageId != null) audioPlaybackManager.stop()
    }

    fun seekVoice(messageId: String, fraction: Float) {
        audioPlaybackManager.seekTo(messageId, fraction)
    }

    fun toggleVoiceSpeed(messageId: String) {
        audioPlaybackManager.toggleSpeed(messageId)
    }

    fun toggleSpeaker() {
        val isSpeakerOn = !_uiState.value.isSpeakerOn
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

    // endregion

    // region 会话监听

    init {
        if (chatId.startsWith("group_")) {
            viewModelScope.launch {
                groupDao.observeById(chatId)
                    .combine(groupDao.observeMembers(chatId)) { group, members ->
                        group to members
                    }
                    .collect { (group, members) ->
                        _uiState.update { current ->
                            current.copy(
                                title = group?.remark?.takeIf(String::isNotBlank)
                                    ?: group?.name.orEmpty(),
                                mentionMembers = members
                                    .filterNot { it.userId == current.myId }
                                    .map { MentionMember(it.userId, it.nickname, it.avatarPath) }
                            )
                        }
                    }
            }
        }
        // 联系人 & 个人资料
        viewModelScope.launch {
            contactRepository.observeContact(chatId)
                .combine(profileRepository.observeProfile()) { contact, profile ->
                    val isLocalAi = chatId == LocalAiAssistant.ID
                    val isSelf = !isLocalAi && chatId == profile?.id
                    _uiState.value.copy(
                        title = if (isLocalAi) LocalAiAssistant.NAME else contact?.displayName
                            ?: if (isSelf) profile.nickname else _uiState.value.title,
                        peerId = if (isLocalAi) LocalAiAssistant.ID else contact?.id ?: chatId,
                        peerAvatar = contact?.avatarPath ?: _uiState.value.peerAvatar,
                        myId = profile?.id,
                        myAvatar = profile?.avatarPath,
                        isSelf = isSelf
                    )
                }.collect { _uiState.value = it }
        }

        // 会话变更 & 聊天背景
        viewModelScope.launch {
            chatSessionRepository.observeSession(chatId)
                .combine(chatSettingsRepository.chatBackground) { session, bg -> session to bg }
                .collect { (session, bg) ->
                    _uiState.update { cur ->
                        cur.copy(
                            title = session?.contactName?.takeIf { !chatId.startsWith("group_") }
                                ?: cur.title,
                            peerId = session?.contactId ?: cur.peerId,
                            peerAvatar = session?.contactAvatar ?: cur.peerAvatar,
                            isSelf = session?.let { it.contactId == cur.myId } ?: cur.isSelf,
                            isMuted = session?.isMuted ?: cur.isMuted,
                            isTemporary = session?.isTemporary == true,
                            isOnline = if (chatId == LocalAiAssistant.ID) {
                                localAiEngine.state.value is LocalAiState.Ready
                            } else {
                                session?.isOnline ?: cur.isOnline
                            },
                            draftMessage = session?.draftMessage ?: cur.draftMessage,
                            backgroundPath = session?.backgroundPath ?: bg
                        )
                    }
                }
        }

        // 扬声器 & 发送按钮设置
        viewModelScope.launch {
            chatSettingsRepository.speakerEnabled
                .combine(chatSettingsRepository.sendButtonEnabled) { speaker, sendButton -> speaker to sendButton }
                .collect { (speaker, sendButton) ->
                    _uiState.update { it.copy(isSpeakerOn = speaker, isSendButtonOn = sendButton) }
                }
        }
    }

    // endregion

    // region 消息操作

    fun clearUnreadState() {
        viewModelScope.launch { messageRepository.markAllAsRead(chatId) }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(chatId.hashCode())
    }

    fun sendMessage(content: MessageContent) {
        viewModelScope.launch {
            if (chatId == LocalAiAssistant.ID && !chatSessionRepository.exists(chatId)) {
                chatSessionRepository.createSession(
                    ChatSession(
                        id = chatId,
                        contactId = chatId,
                        contactName = LocalAiAssistant.NAME
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
                    soundTipPlayer.play(R.raw.tip_after_upload_voice)
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
            val receiverId = _uiState.value.myId ?: return@launch
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
                val text = error.message ?: "本地模型推理失败"
                if (response.isEmpty()) {
                    response.append(text)
                } else {
                    response.append("\n\n生成中断：").append(text)
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

    fun finishAiStreamHandoff(messageId: String) {
        _streamingAiMessage.update { current ->
            if (current?.id == messageId && !current.isGenerating) null else current
        }
    }

    fun retrySend(messageId: String) {
        viewModelScope.launch { messageRepository.retrySend(messageId) }
    }

    fun saveDraftMessage(draft: String) {
        viewModelScope.launch {
            chatSessionRepository.updateDraft(
                sessionId = chatId,
                draft = draft.takeIf { it.isNotBlank() }
            )
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { messageRepository.deleteMessage(messageId) }
    }

    fun recallMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.recallMessage(messageId).onFailure {
                context.showToast(it.message ?: context.getString(R.string.msg_process_failed))
            }
        }
    }

    fun saveFile(message: ChatMessage) {
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

    fun pauseTransfer(messageId: String) {
        viewModelScope.launch {
            messageRepository.pauseTransfer(messageId).onFailure {
                context.showToast(context.getString(R.string.msg_process_failed))
            }
        }
    }

    fun resumeTransfer(messageId: String) {
        viewModelScope.launch {
            messageRepository.resumeTransfer(messageId).onFailure {
                context.showToast(context.getString(R.string.msg_process_failed))
            }
        }
    }

    fun cancelTransfer(messageId: String) {
        viewModelScope.launch { messageRepository.cancelTransfer(messageId) }
    }

    fun reeditMessage(text: String) = emit(MessageUiEvent.ReeditMessage(text))

    // endregion

    // region 消息点击

    fun handleMessageClick(message: ChatMessage) {
        when (val content = message.content) {
            is MessageContent.Image,
            is MessageContent.Video -> if (content.localPath.isNotBlank()) openMediaPreview(message)

            is MessageContent.Voice -> toggleVoicePlay(message.id, content.localPath)
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

    // endregion

    // region 多选操作

    fun isMessageSelected(messageId: String) = messageId in _uiState.value.selectedMessageIds

    fun enterSelectMode(messageId: String) {
        _uiState.update { it.copy(isSelectMode = true, selectedMessageIds = setOf(messageId)) }
    }

    fun exitSelectMode() {
        _uiState.update { it.copy(isSelectMode = false, selectedMessageIds = emptySet()) }
    }

    fun toggleMessageSelection(messageId: String) {
        _uiState.update { state ->
            val newIds = if (messageId in state.selectedMessageIds) {
                state.selectedMessageIds - messageId
            } else {
                state.selectedMessageIds + messageId
            }
            state.copy(selectedMessageIds = newIds)
        }
    }

    fun deleteSelectedMessages() {
        val ids = _uiState.value.selectedMessageIds
        viewModelScope.launch { messageRepository.deleteMessages(ids, chatId) }
        exitSelectMode()
    }

    fun saveSelectedMessageFiles() {
        val ids = _uiState.value.selectedMessageIds
        exitSelectMode()

        viewModelScope.launch {
            val contents = ids.mapNotNull { id ->
                messages.value.find { it.id == id }?.content?.takeIf { it.getLocalPath() != null }
            }
            if (contents.isEmpty()) {
                context.showToast("没有找到可以保存的内容")
                return@launch
            }

            _uiState.update { it.copy(isFullscreenLoading = true) }

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

            _uiState.update { it.copy(isFullscreenLoading = false) }
            context.showToast(
                when {
                    failCount == 0 -> "已保存 $successCount 个文件"
                    successCount == 0 -> "保存失败"
                    else -> "已保存 $successCount 个文件，$failCount 个失败"
                }
            )
        }
    }

    fun forwardMessages(targetChatIds: Set<String>) {
        val ids = _uiState.value.selectedMessageIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            messageRepository.forwardMessages(ids, targetChatIds)
            context.showToast("已发送")
        }
        exitSelectMode()
    }

    fun forwardMergedMessages(targetChatIds: Set<String>) {
        val ids = _uiState.value.selectedMessageIds
        if (ids.isEmpty()) return
        val title = "${_uiState.value.title}的聊天记录"
        viewModelScope.launch {
            messageRepository.forwardMergedMessages(
                ids = ids,
                targetChatIds = targetChatIds,
                historyTitle = title,
                myName = "我",
                peerName = _uiState.value.title
            )
            context.showToast("已发送")
        }
        exitSelectMode()
    }

    fun handleMultiSelectAction(action: MultiMessageAction) {
        when (action) {
            MultiMessageAction.Forward -> emit(MessageUiEvent.ForwardMessage())
            MultiMessageAction.Delete -> emit(MessageUiEvent.ShowDeleteConfirm())
            MultiMessageAction.Download -> emit(MessageUiEvent.ShowDownloadConfirm)
            MultiMessageAction.Favorite -> viewModelScope.launch {
                val messages = _uiState.value.selectedMessageIds.mapNotNull {
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
                    sourceName = _uiState.value.title,
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

    // endregion

    // region 跳转联系人

    /**
     * 跳转到联系人详情前的准备工作：
     * - 若对方是自己或已是好友，直接返回成功，跳转到联系人详情；
     * - 否则预拉取对方资料，供「申请添加好友」页使用。
     */
    suspend fun prepareRequestAddFriend(
        userId: String = chatId,
        fromContactCard: Boolean = false
    ): Result<Unit> {
        if (fromContactCard && (userId == _uiState.value.myId || contactRepository.exists(userId))) {
            return Result.success(Unit)
        }

        _uiState.update { it.copy(isFullscreenLoading = true) }
        return runCatching {
            if (addFriendRepository.fetchProfile(userId) == null) {
                context.showToast(context.getString(R.string.add_contact_fetch_profile_failed))
                error("failed to fetch profile for $userId")
            }
        }.also {
            _uiState.update { it.copy(isFullscreenLoading = false) }
        }
    }

    // endregion

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
