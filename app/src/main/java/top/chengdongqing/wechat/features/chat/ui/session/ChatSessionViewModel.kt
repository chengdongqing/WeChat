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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.designsystem.components.location.preview.previewLocation
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.components.media.preview.previewMedias
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastState
import top.chengdongqing.wechat.core.file.PublicFileManager
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.notification.NotificationHelper
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.chat.data.mapper.getLocalPath
import top.chengdongqing.wechat.features.chat.data.mapper.toMediaItem
import top.chengdongqing.wechat.features.chat.data.mapper.toMessageType
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.chat.ui.session.message.ChatSessionUiState
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageAction
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.features.chat.ui.session.message.MultiMessageAction
import top.chengdongqing.wechat.features.chat.ui.session.message.toolbar.MessageToolbarManager
import top.chengdongqing.wechat.features.chat.util.AudioPlaybackManager
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import top.chengdongqing.wechat.features.me.domain.repository.SettingsRepository
import java.io.File
import kotlin.time.Duration

@HiltViewModel(assistedFactory = ChatSessionViewModel.Factory::class)
class ChatSessionViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val contactP2PRepository: ContactP2PRepository,
    private val publicFileManager: PublicFileManager,
    private val soundTipPlayer: SoundTipPlayer,
    private val notificationHelper: NotificationHelper,
    val activeSessionManager: ActiveSessionManager,
    e2eSessionManager: E2ESessionManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatSessionViewModel
    }

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(ChatSessionUiState())
    val uiState = _uiState.asStateFlow()

    private val _visibleCount = MutableStateFlow(PAGE_SIZE)

    // region 工具条

    private val _uiEvent = MutableSharedFlow<MessageUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val toolbarManager = MessageToolbarManager(
        context = context,
        scope = viewModelScope,
        uiEvent = _uiEvent,
        onRecallMessage = ::recallMessage,
        onToggleSpeaker = ::toggleSpeaker,
        onSaveFile = ::saveFile,
        onMultiSelect = ::enterSelectMode
    )

    val toolbarState = toolbarManager.state

    fun handleMessageLongPress(
        message: ChatMessage,
        bubblePosition: Offset,
        bubbleHeight: Float
    ) {
        toolbarManager.onLongPress(
            message = message,
            bubblePosition = bubblePosition,
            bubbleHeight = bubbleHeight,
            isSpeakerOn = _uiState.value.isSpeakerOn
        )
    }

    fun handleToolbarAction(action: MessageAction) {
        toolbarManager.onAction(action)
    }

    fun dismissToolbar() {
        toolbarManager.dismiss()
    }

    // endregion

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
     * 媒体预览列表
     *
     * 通过特征列表做 distinctUntilChanged，
     * 只有消息 ID 或媒体属性真正变化时才重新计算索引。
     */
    private val mediaState = messages
        .map { list ->
            list.map { it.id to (it.content is MessageContent.Media) }
        }
        .distinctUntilChanged()
        .map {
            val allMessages = messages.value
            withContext(Dispatchers.Default) {
                buildMediaState(allMessages)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MediaState()
        )

    private fun buildMediaState(allMessages: List<ChatMessage>): MediaState {
        val mediaItems = mutableListOf<MediaItem>()
        val idToIndexMap = mutableMapOf<String, Int>()

        for (i in allMessages.indices.reversed()) {
            val message = allMessages[i]
            (message.content as? MessageContent.Media)?.toMediaItem()?.let {
                mediaItems.add(it)
                idToIndexMap[message.id] = mediaItems.size - 1
            }
        }

        return MediaState(list = mediaItems, indexMap = idToIndexMap)
    }

    // endregion

    // region 加密 & 未读

    val isE2EActive = e2eSessionManager.encryptedPeers
        .map { it.contains(chatId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val unreadCount = chatSessionRepository.observeTotalUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    // endregion

    // region 语音播放

    private val audioPlaybackManager = AudioPlaybackManager(
        context = context,
        soundTipPlayer = soundTipPlayer,
        onPlayingStateChanged = { _playingMessageId.value = it },
        onMessagePlayed = { markAsPlayed(it) }
    )

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId = _playingMessageId.asStateFlow()

    fun toggleVoicePlay(messageId: String, localPath: String) {
        val voiceMessages = messages.value.filter { it.content is MessageContent.Voice }
        audioPlaybackManager.togglePlay(
            messageId = messageId,
            localPath = localPath,
            messages = voiceMessages,
            isSpeakerOn = _uiState.value.isSpeakerOn
        )
    }

    fun stopVoice() {
        if (_playingMessageId.value != null) {
            audioPlaybackManager.stop()
        }
    }

    private fun markAsPlayed(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messages.value.find { it.id == messageId }?.let { message ->
                if (message.content.showUnreadDot) {
                    messageRepository.markVoiceAsPlayed(messageId)
                }
            }
        }
    }

    // endregion

    // region 会话监听

    private val sessionFlow = chatSessionRepository.observeSession(chatId)

    init {
        loadInitialData()
        observeSessionChanges()
        observeSettings()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val contact = contactRepository.getContactById(chatId)
            val profile = profileRepository.getProfile()

            _uiState.update {
                it.copy(
                    title = contact?.displayName ?: profile?.nickname ?: "",
                    peerId = contact?.id,
                    peerAvatar = contact?.avatarPath,
                    myId = profile?.id,
                    myAvatar = profile?.avatarPath,
                    isSelf = contact == null
                )
            }
        }
    }

    private fun observeSessionChanges() {
        viewModelScope.launch {
            sessionFlow.collect { session ->
                session?.let { s ->
                    _uiState.update {
                        it.copy(
                            backgroundPath = s.backgroundPath,
                            isMuted = s.isMuted,
                            isOnline = s.isOnline,
                            draftMessage = s.draftMessage
                        )
                    }
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.speakerEnabled.collect { isOn ->
                _uiState.update {
                    it.copy(
                        isSpeakerOn = isOn
                    )
                }
            }
        }
    }

    // endregion

    // region 消息操作

    fun clearUnreadState() {
        viewModelScope.launch {
            messageRepository.markAllAsRead(chatId)
        }
        notificationHelper.cancelNotification(chatId.hashCode())
    }

    fun loadMore() {
        if (messages.value.size < PAGE_SIZE
            || _uiState.value.isLoadingMore
            || !_uiState.value.hasMoreMessages
        ) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val newCount = _visibleCount.value + PAGE_SIZE
            _visibleCount.value = newCount

            val hasMore = checkHasMore()

            delay(1000)
            _uiState.update {
                it.copy(isLoadingMore = false, hasMoreMessages = hasMore)
            }
        }
    }

    private suspend fun checkHasMore(): Boolean {
        val oldestTimestamp = messages.value.lastOrNull()?.timestamp ?: return false
        return messageRepository.hasOlderMessages(chatId, oldestTimestamp)
    }

    fun sendMessage(content: MessageContent) {
        viewModelScope.launch {
            messageRepository.sendMessage(
                sessionId = chatId,
                receiverId = chatId,
                content = content
            ).onSuccess {
                if (content is MessageContent.Voice) {
                    soundTipPlayer.play(R.raw.after_upload_voice)
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
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun recallMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.recallMessage(messageId).onFailure {
                context.showToast(it.message!!)
            }
        }
    }

    fun toggleSpeaker() {
        val isSpeakerOn = !_uiState.value.isSpeakerOn
        viewModelScope.launch {
            settingsRepository.toggleSpeaker(isSpeakerOn)
        }
    }

    fun saveFile(message: ChatMessage) {
        val localPath = message.content.getLocalPath() ?: return
        val file = File(localPath)

        viewModelScope.launch {
            val res = publicFileManager.saveMedia(
                messageType = message.content.toMessageType(),
                sourceFile = file,
                filename = file.name
            )

            res?.let {
                context.showToast("已保存到本地")
            } ?: run {
                context.showToast("保存失败")
            }
        }
    }

    fun stopTransfer(messageId: String) {
        viewModelScope.launch { messageRepository.stopTransfer(messageId) }
    }

    fun reeditMessage(text: String) {
        viewModelScope.launch {
            _uiEvent.emit(MessageUiEvent.ReeditMessage(text))
        }
    }

    // endregion

    // region 消息点击

    fun handleMessageClick(message: ChatMessage) {
        when (message.content) {
            is MessageContent.Image,
            is MessageContent.Video -> openMediaPreview(message)

            is MessageContent.Voice ->
                toggleVoicePlay(message.id, message.content.localPath)

            is MessageContent.File -> viewModelScope.launch {
                _uiEvent.emit(MessageUiEvent.PreviewFile(message.id))
            }

            is MessageContent.Call -> viewModelScope.launch {
                _uiEvent.emit(MessageUiEvent.LaunchCall(message.content.type))
            }

            is MessageContent.Location -> openLocationPreview(message.content)

            else -> {}
        }
    }

    private fun openMediaPreview(message: ChatMessage) {
        val (mediaList, indexMap) = mediaState.value
        val index = indexMap[message.id] ?: run {
            Log.e("MediaPreview", "找不到该消息的媒体索引: ${message.id}")
            return
        }
        context.previewMedias(mediaList, index)
    }

    private fun openLocationPreview(content: MessageContent.Location) {
        val info = LocationPreviewInfo(
            coordinate = LatLng(content.latitude, content.longitude),
            address = content.address,
            name = content.poiName
        )
        context.previewLocation(info)
    }

    // endregion

    // region 联系人缓存

    fun prepareRequestAddFriend() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.let { state ->
                val contact = Contact(
                    id = state.peerId!!,
                    nickname = state.title,
                    avatarPath = state.peerAvatar
                )
                contactP2PRepository.setContactToCache(
                    contactId = chatId,
                    contact = contact
                )
            }
        }
    }

    // endregion

    // region 消息多选

    fun isMessageSelected(messageId: String): Boolean {
        return messageId in _uiState.value.selectedMessageIds
    }

    fun enterSelectMode(messageId: String) {
        _uiState.update {
            it.copy(
                isSelectMode = true,
                selectedMessageIds = setOf(messageId)
            )
        }
    }

    fun exitSelectMode() {
        _uiState.update {
            it.copy(
                isSelectMode = false,
                selectedMessageIds = emptySet()
            )
        }
    }

    fun toggleMessageSelection(messageId: String) {
        _uiState.update {
            val newSet = if (messageId in it.selectedMessageIds) {
                it.selectedMessageIds - messageId
            } else {
                it.selectedMessageIds + messageId
            }
            it.copy(selectedMessageIds = newSet)
        }
    }

    fun deleteSelectedMessages() {
        val ids = _uiState.value.selectedMessageIds

        viewModelScope.launch {
            messageRepository.deleteMessages(ids)
        }

        exitSelectMode()
    }

    fun saveSelectedMessageFiles(toast: ToastState) {
        val ids = _uiState.value.selectedMessageIds
        exitSelectMode()

        viewModelScope.launch {
            // 过滤出所有有本地文件路径的消息内容
            val contents = ids.mapNotNull { id ->
                messages.value.find {
                    it.id == id
                }?.content.takeIf {
                    it?.getLocalPath() != null
                }
            }
            if (contents.isEmpty()) {
                context.showToast("没有找到可以保存的内容")
                return@launch
            }

            toast.show(
                "处理中...",
                ToastIcon.Loading,
                duration = Duration.INFINITE,
                mask = true
            )

            // 并发保存所有文件，等待全部完成
            val results = contents.map { content ->
                async {
                    val file = File(content.getLocalPath()!!)
                    publicFileManager.saveMedia(
                        messageType = content.toMessageType(),
                        sourceFile = file,
                        filename = file.name
                    )
                }
            }.awaitAll()

            // 汇总结果，统一提示
            val successCount = results.count { it != null }
            val failCount = results.size - successCount

            toast.hide()
            context.showToast(
                when {
                    failCount == 0 -> "已保存 $successCount 个文件"
                    successCount == 0 -> "保存失败"
                    else -> "已保存 $successCount 个文件，$failCount 个失败"
                }
            )
        }
    }

    fun forwardSelectedMessages(targetChatIds: Set<String>) {
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
            MultiMessageAction.Forward -> {
                viewModelScope.launch {
                    _uiEvent.emit(MessageUiEvent.ForwardMessage())
                }
            }

            MultiMessageAction.Delete -> {
                viewModelScope.launch {
                    _uiEvent.emit(MessageUiEvent.ShowDeleteConfirm())
                }
            }

            MultiMessageAction.Download -> {
                viewModelScope.launch {
                    _uiEvent.emit(MessageUiEvent.ShowDownloadConfirm)
                }
            }

            else -> {}
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