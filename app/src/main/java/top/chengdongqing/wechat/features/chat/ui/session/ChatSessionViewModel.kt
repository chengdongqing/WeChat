package top.chengdongqing.wechat.features.chat.ui.session

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
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
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.util.copyToClipboard
import top.chengdongqing.wechat.core.util.isWithinMinutes
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.notification.NotificationHelper
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.chat.data.mapper.toMediaItem
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageAction
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageToolbarState
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.features.chat.util.AudioPlaybackManager
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

@HiltViewModel(assistedFactory = ChatSessionViewModel.Factory::class)
class ChatSessionViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
    private val contactP2PRepository: ContactP2PRepository,
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

    /**
     * 工具条状态
     */
    private val _toolbarState = MutableStateFlow(MessageToolbarState())
    val toolbarState = _toolbarState.asStateFlow()

    /**
     * 消息流
     */
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
     */
    private val mediaState = messages
        .map { list ->
            // 提取一个只包含【ID + 是否是媒体内容】的特征列表
            // 只有当这个“特征”变了，下游才需要重新计算 Map
            list.map { it.id to (it.content is MessageContent.Media) }
        }
        .distinctUntilChanged()
        .map {
            val allMessages = messages.value
            withContext(Dispatchers.Default) {
                val mediaItems = mutableListOf<MediaItem>()
                val idToIndexMap = mutableMapOf<String, Int>()

                // 从后往前遍历：一次性完成 过滤 + 转换 + 倒序
                for (i in allMessages.indices.reversed()) {
                    val message = allMessages[i]
                    (message.content as? MessageContent.Media)?.toMediaItem()?.let {
                        mediaItems.add(it)
                        idToIndexMap[message.id] = mediaItems.size - 1
                    }
                }

                MediaState(
                    list = mediaItems,
                    indexMap = idToIndexMap
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MediaState()
        )

    /**
     * 是否启用了加密
     */
    val isE2EActive = e2eSessionManager.encryptedPeers
        .map { it.contains(chatId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    /**
     * 未读数
     */
    val unreadCount = chatSessionRepository.observeTotalUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    private val sessionFlow = chatSessionRepository.observeSession(chatId)

    private val audioPlaybackManager = AudioPlaybackManager(
        context = context,
        soundTipPlayer = soundTipPlayer,
        onPlayingStateChanged = { _playingMessageId.value = it },
        onMessagePlayed = { markAsPlayed(it) }
    )

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId = _playingMessageId.asStateFlow()

    /**
     * UI事件流（一次性事件）
     */
    private val _uiEvent = MutableSharedFlow<MessageUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    /**
     * 多选模式状态
     */
    private val _multiSelectMode = MutableStateFlow(false)
    val multiSelectMode = _multiSelectMode.asStateFlow()

    private val _selectedMessages = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessages = _selectedMessages.asStateFlow()

    init {
        loadInitialData()
        observeSessionChanges()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val contact = contactRepository.getContactById(chatId)
            val profile = profileRepository.getCurrentProfileSnapshot()

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

    fun clearUnreadState() {
        viewModelScope.launch {
            messageRepository.markAllAsRead(chatId)
        }
        notificationHelper.cancelNotification(chatId.hashCode())
    }

    private fun observeSessionChanges() {
        viewModelScope.launch {
            sessionFlow.collect { session ->
                session?.let { s ->
                    _uiState.update {
                        it.copy(
                            backgroundPath = s.backgroundPath,
                            isMuted = s.isMuted,
                            isSpeakerOn = s.isSpeakerOn,
                            isOnline = s.isOnline,
                            draftMessage = s.draftMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * 加载更多历史
     */
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
                it.copy(
                    isLoadingMore = false,
                    hasMoreMessages = hasMore
                )
            }
        }
    }

    private suspend fun checkHasMore(): Boolean {
        val oldestTimestamp = messages.value.lastOrNull()?.timestamp ?: return false
        return messageRepository.hasOlderMessages(chatId, oldestTimestamp)
    }

    /**
     * 发送消息
     */
    fun sendMessage(content: MessageContent) {
        viewModelScope.launch {
            messageRepository.sendMessage(
                sessionId = chatId,
                receiverId = chatId,
                content = content
            ).onSuccess {
                when {
                    content is MessageContent.Voice -> {
                        soundTipPlayer.play(R.raw.after_upload_voice)
                    }
                }
            }
        }
    }

    /**
     * 重试发送失败的消息
     */
    fun retrySend(messageId: String) {
        viewModelScope.launch {
            messageRepository.retrySend(messageId)
        }
    }

    /**
     * 保存草稿消息
     */
    fun saveDraftMessage(draft: String) {
        viewModelScope.launch {
            chatSessionRepository.updateDraft(
                sessionId = chatId,
                draft = draft.takeIf { it.isNotBlank() }
            )
        }
    }

    /**
     * 删除消息
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    /**
     * 撤回消息
     */
    fun recallMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.recallMessage(messageId).onFailure {
                context.showToast(it.message!!)
            }
        }
    }

    /**
     * 切换语音播放模式
     */
    fun toggleSpeaker() {
        val isSpeakerOn = !_uiState.value.isSpeakerOn
        viewModelScope.launch {
            chatSessionRepository.toggleSpeaker(chatId, isSpeakerOn)
        }
    }

    /**
     * 停止文件传输
     */
    fun stopTransfer(messageId: String) {
        viewModelScope.launch {
            messageRepository.stopTransfer(messageId)
        }
    }

    /**
     * 重新编辑消息
     */
    fun reeditMessage(text: String) {
        viewModelScope.launch {
            _uiEvent.emit(MessageUiEvent.ReeditMessage(text))
        }
    }

    /**
     * 控制语音 播放/停止
     */
    fun toggleVoicePlay(messageId: String, localPath: String) {
        val voiceMessages = messages.value.filter { it.content is MessageContent.Voice }

        audioPlaybackManager.togglePlay(
            messageId = messageId,
            localPath = localPath,
            messages = voiceMessages,
            isSpeakerOn = _uiState.value.isSpeakerOn
        )
    }

    /**
     * 停止播放语音
     */
    fun stopVoice() {
        if (_playingMessageId.value != null) {
            audioPlaybackManager.stop()
        }
    }

    private fun markAsPlayed(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messages.value.find { it.id == messageId }?.let { message ->
                if (message.content.showUnreadDot) {
                    viewModelScope.launch {
                        messageRepository.markVoiceAsPlayed(messageId)
                    }
                }
            }
        }
    }

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

    /**
     * 处理消息点击事件
     */
    fun handleMessageClick(message: ChatMessage) {
        when (val content = message.content) {
            // 预览图片/视频
            is MessageContent.Image,
            is MessageContent.Video -> {
                val (mediaList, indexMap) = mediaState.value
                val index = indexMap[message.id] ?: run {
                    Log.e("MediaPreview", "找不到该消息的媒体索引: ${message.id}")
                    return
                }
                context.previewMedias(mediaList, index)
            }

            // 开始/停止播放语音
            is MessageContent.Voice -> {
                toggleVoicePlay(message.id, message.content.localPath)
            }

            // 预览文件
            is MessageContent.File -> {
                viewModelScope.launch {
                    _uiEvent.emit(MessageUiEvent.PreviewFile(message.id))
                }
            }

            // 调起通话
            is MessageContent.Call -> {
                viewModelScope.launch {
                    _uiEvent.emit(MessageUiEvent.LaunchCall(message.content.type))
                }
            }

            // 预览位置
            is MessageContent.Location -> {
                val location = LocationPreviewInfo(
                    coordinate = LatLng(
                        content.latitude,
                        content.longitude
                    ),
                    address = content.address,
                    name = content.poiName
                )
                context.previewLocation(location)
            }

            else -> {}
        }
    }

    /**
     * 处理消息长按事件
     */
    fun handleMessageLongPress(
        message: ChatMessage,
        position: Offset,
        bubblePosition: Offset,
        bubbleHeight: Float
    ) {
        val actions = getAvailableActions(message, _uiState.value.isSpeakerOn)

        /**
         * 文本消息特殊处理：默认全选文本
         */
        if (message.content is MessageContent.Text) {
            val textContent = message.content
            val fullSelection = TextRange(0, textContent.text.length)

            _toolbarState.update {
                it.copy(
                    visible = true,
                    message = message,
                    position = position,
                    bubblePosition = bubblePosition,
                    bubbleHeight = bubbleHeight,
                    actions = actions,
                    textSelection = fullSelection,
                    selectedText = textContent.text
                )
            }
        } else {
            _toolbarState.update {
                it.copy(
                    visible = true,
                    message = message,
                    position = position,
                    bubblePosition = bubblePosition,
                    bubbleHeight = bubbleHeight,
                    actions = actions
                )
            }
        }
    }

    /**
     * 处理文本选择变化
     */
    fun handleTextSelectionChange(selection: TextRange) {
        val currentState = _toolbarState.value
        if (currentState.message?.content is MessageContent.Text) {
            val textContent = currentState.message.content
            val selectedText = textContent.text.substring(selection.start, selection.end)

            _toolbarState.update {
                it.copy(
                    textSelection = selection,
                    selectedText = selectedText,
                    position = Offset.Zero // 选择变化时重新计算位置
                )
            }
        }
    }

    /**
     * 隐藏工具条
     */
    fun dismissToolbar() {
        _toolbarState.update {
            MessageToolbarState()
        }
    }

    /**
     * 处理工具条操作
     */
    fun handleToolbarAction(action: MessageAction) {
        val state = _toolbarState.value
        val message = state.message ?: return

        when (action) {
            MessageAction.Copy -> {
                state.selectedText?.let {
                    context.copyToClipboard(it, "message")
                    context.showToast("已复制")
                }
            }

            MessageAction.Delete -> {
                viewModelScope.launch {
                    _uiEvent.emit(MessageUiEvent.ShowDeleteConfirm(message.id))
                }
            }

            MessageAction.Recall -> {
                recallMessage(message.id)
            }

            MessageAction.SpeakerMode,
            MessageAction.EarpieceMode -> {
                toggleSpeaker()
            }

            else -> {}
        }

        dismissToolbar()
    }

    /**
     * 获取消息可用的操作列表
     */
    private fun getAvailableActions(
        message: ChatMessage,
        isSpeakerOn: Boolean = true
    ): List<MessageAction> {
        val allowRecall = message.isFromMe && message.timestamp.isWithinMinutes()
        val deleteOrRecall = if (allowRecall) MessageAction.Recall else MessageAction.Delete

        return buildList {
            when (message.content) {
                is MessageContent.Text -> {
                    add(MessageAction.Copy)
                    add(MessageAction.Forward)
                    add(MessageAction.Favorite)
                    add(deleteOrRecall)
                    add(MessageAction.MultiSelect)
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                }

                is MessageContent.Voice -> {
                    add(if (isSpeakerOn) MessageAction.EarpieceMode else MessageAction.SpeakerMode)
                    add(MessageAction.Favorite)
                    add(MessageAction.Quote)
                    add(deleteOrRecall)
                    add(MessageAction.MultiSelect)
                    add(MessageAction.Remind)
                }

                is MessageContent.Sticker -> {
                    add(MessageAction.Forward)
                    add(deleteOrRecall)
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                    add(MessageAction.MultiSelect)
                }

                is MessageContent.Call -> {
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                    add(deleteOrRecall)
                }

                is MessageContent.ContactCard -> {
                    add(MessageAction.Forward)
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                    add(deleteOrRecall)
                    add(MessageAction.MultiSelect)
                }

                is MessageContent.Image,
                is MessageContent.Video,
                is MessageContent.Location,
                is MessageContent.Favorite,
                is MessageContent.File -> {
                    add(MessageAction.Forward)
                    add(MessageAction.Favorite)
                    add(MessageAction.Quote)
                    add(deleteOrRecall)
                    add(MessageAction.MultiSelect)
                    add(MessageAction.Remind)
                }

                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackManager.release()
    }
}

data class ChatSessionUiState(
    val title: String = "",
    val peerId: String? = null,
    val peerAvatar: String? = null,
    val myId: String? = null,
    val myAvatar: String? = null,
    val isSelf: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val backgroundPath: String? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isOnline: Boolean = false,
    val draftMessage: String? = null
)

private data class MediaState(
    val list: List<MediaItem> = emptyList(),
    val indexMap: Map<String, Int> = emptyMap()
)