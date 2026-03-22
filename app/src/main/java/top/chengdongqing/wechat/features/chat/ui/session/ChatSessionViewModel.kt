package top.chengdongqing.wechat.features.chat.ui.session

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.model.LatLng
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.file.PublicFileManager
import top.chengdongqing.wechat.core.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.location.preview.previewLocation
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.core.media.preview.previewMedias
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtBondManager
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.service.notification.NotificationHelper
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.chat.data.mapper.getLocalPath
import top.chengdongqing.wechat.features.chat.data.mapper.toMediaItem
import top.chengdongqing.wechat.features.chat.data.mapper.toMessageType
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageAction
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.features.chat.ui.session.message.MultiMessageAction
import top.chengdongqing.wechat.features.chat.ui.session.message.toolbar.MessageToolbarManager
import top.chengdongqing.wechat.features.chat.ui.session.util.AudioPlaybackManager
import top.chengdongqing.wechat.features.contacts.domain.repository.AddFriendRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.profile.domain.repository.ProfileRepository
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.ConnectionSettingsRepository
import java.io.File

@HiltViewModel(assistedFactory = ChatSessionViewModel.Factory::class)
class ChatSessionViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val profileRepository: ProfileRepository,
    private val chatSettingsRepository: ChatSettingsRepository,
    private val contactRepository: ContactRepository,
    private val addFriendRepository: AddFriendRepository,
    private val publicFileManager: PublicFileManager,
    private val soundTipPlayer: SoundTipPlayer,
    private val notificationHelper: NotificationHelper,
    private val chatTransportManager: ChatTransportManager,
    private val btBondManager: BtBondManager,
    private val activeSessionManager: ActiveSessionManager,
    e2eSessionManager: E2ESessionManager,
    connectionSettingsRepository: ConnectionSettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatSessionViewModel
    }

    companion object {
        private const val PAGE_SIZE = 20

        /** 加载更多指示器最短展示时间，防止列表闪烁 */
        private const val LOAD_MORE_INDICATOR_DELAY_MS = 1000L
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

    /** 当前页面加载消息的条数游标 */
    private val _visibleCount = MutableStateFlow(PAGE_SIZE)

    /** 在新协程中发射 UI 事件，省去调用侧的样板代码 */
    private fun emit(event: MessageUiEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }

    // region 消息流

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _visibleCount.flatMapLatest { count ->
        messageRepository.observeMessages(chatId, count)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
        val indexMap = mutableMapOf<String, Int>()
        for (i in messages.indices.reversed()) {
            (messages[i].content as? MessageContent.Media)?.toMediaItem()?.let {
                items.add(it)
                indexMap[messages[i].id] = items.lastIndex
            }
        }
        return MediaState(list = items, indexMap = indexMap)
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
    suspend fun isBluetoothDeviceSaved() = btBondManager.hasSaved(chatId)

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
        onMultiSelect = ::enterSelectMode
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
        }
        toolbarManager.onAction(action)
    }

    fun dismissToolbar() = toolbarManager.dismiss()

    // endregion

    // region 语音播放

    private val audioPlaybackManager = AudioPlaybackManager(
        context = context,
        scope = viewModelScope,
        soundTipPlayer = soundTipPlayer,
        onPlayingStateChanged = { _playingMessageId.value = it },
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
        if (_playingMessageId.value != null) audioPlaybackManager.stop()
    }

    fun toggleSpeaker() {
        viewModelScope.launch {
            chatSettingsRepository.toggleSpeaker(!_uiState.value.isSpeakerOn)
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
        // 联系人 & 个人资料
        viewModelScope.launch {
            contactRepository.observeContact(chatId)
                .combine(profileRepository.observeProfile()) { contact, profile ->
                    _uiState.value.copy(
                        title = contact?.displayName ?: profile?.nickname ?: "",
                        peerId = contact?.id,
                        peerAvatar = contact?.avatarPath,
                        myId = profile?.id,
                        myAvatar = profile?.avatarPath,
                        isSelf = contact == null
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
                            peerAvatar = session?.contactAvatar ?: cur.peerAvatar,
                            isMuted = session?.isMuted ?: cur.isMuted,
                            isOnline = session?.isOnline ?: cur.isOnline,
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
        notificationHelper.cancelNotification(chatId.hashCode())
    }

    fun loadMore() {
        val canLoad = messages.value.size >= PAGE_SIZE
                && !_uiState.value.isLoadingMore
                && _uiState.value.hasMoreMessages
        if (!canLoad) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            _visibleCount.value += PAGE_SIZE
            val hasMore = checkHasMore()
            delay(LOAD_MORE_INDICATOR_DELAY_MS)
            _uiState.update { it.copy(isLoadingMore = false, hasMoreMessages = hasMore) }
        }
    }

    private suspend fun checkHasMore(): Boolean {
        val oldest = messages.value.lastOrNull()?.timestamp ?: return false
        return messageRepository.hasOlderMessages(chatId, oldest)
    }

    fun sendMessage(content: MessageContent) {
        viewModelScope.launch {
            messageRepository.sendMessage(
                sessionId = chatId,
                receiverId = chatId,
                content = content
            ).onSuccess {
                if (content is MessageContent.Voice) {
                    soundTipPlayer.play(R.raw.tip_after_upload_voice)
                }
            }
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
                MessageUiEvent.PreviewMusic(message.id, content.music.name)
            )

            is MessageContent.Call -> emit(MessageUiEvent.LaunchCall(content.type))
            is MessageContent.Location -> openLocationPreview(content)
            is MessageContent.ContactCard -> viewModelScope.launch {
                val userId = content.userId
                prepareRequestAddFriend(userId = userId, fromContactCard = true)
                    .onSuccess { _uiEvent.emit(MessageUiEvent.NavigateToContact(userId)) }
            }

            else -> {}
        }
    }

    private fun openMediaPreview(message: ChatMessage) {
        val index = mediaState.value.indexMap[message.id] ?: run {
            Log.e("MediaPreview", "找不到该消息的媒体索引: ${message.id}")
            return
        }
        context.previewMedias(mediaState.value.list, index)
    }

    private fun openLocationPreview(content: MessageContent.Location) {
        context.previewLocation(
            LocationPreviewInfo(
                coordinate = LatLng(content.latitude, content.longitude),
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

    fun handleMultiSelectAction(action: MultiMessageAction) {
        when (action) {
            MultiMessageAction.Forward -> emit(MessageUiEvent.ForwardMessage())
            MultiMessageAction.Delete -> emit(MessageUiEvent.ShowDeleteConfirm())
            MultiMessageAction.Download -> emit(MessageUiEvent.ShowDownloadConfirm)
            MultiMessageAction.Favorite -> { /* 收藏功能暂未实现 */
            }
        }
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
        super.onCleared()
        audioPlaybackManager.release()
    }
}

private data class MediaState(
    val list: List<MediaItem> = emptyList(),
    val indexMap: Map<String, Int> = emptyMap()
)