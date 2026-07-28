package top.chengdongqing.wechat.feature.chat.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.repository.MessageRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.network.messaging.GroupLiveEventBus

data class LiveRoomUiState(
    val hasEnded: Boolean = false,
    val viewerCount: Int = 0,
    val viewers: Map<String, String> = emptyMap()
)

@HiltViewModel(assistedFactory = LiveRoomViewModel.Factory::class)
class LiveRoomViewModel @AssistedInject constructor(
    @Assisted("groupId") private val groupId: String,
    @Assisted("liveId") private val liveId: String,
    @Assisted("hostId") private val hostId: String,
    private val profileRepository: ProfileRepository,
    private val messageRepository: MessageRepository,
    private val liveEventBus: GroupLiveEventBus,
    val webRtcManager: LiveWebRtcManager,
    private val json: Json
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("groupId") groupId: String,
            @Assisted("liveId") liveId: String,
            @Assisted("hostId") hostId: String
        ): LiveRoomViewModel
    }

    private val _state = MutableStateFlow(LiveRoomUiState())
    val state = _state.asStateFlow()
    private var heartbeatJob: Job? = null
    private var watchdogJob: Job? = null
    private var lastHostHeartbeat = System.currentTimeMillis()

    init {
        webRtcManager.initialize(::sendSignal)
        viewModelScope.launch {
            liveEventBus.events.collect { live ->
                    if (live.groupId != groupId || live.liveId != liveId) return@collect
                    if (live.senderId == hostId) lastHostHeartbeat = System.currentTimeMillis()
                    when (live.status) {
                        STATUS_JOINED -> live.senderId.let { id ->
                            val updatedViewers = _state.value.viewers +
                                (id to live.displayName)
                            val amHost = profileRepository.requireUserId() == hostId
                            _state.value = _state.value.copy(
                                viewers = updatedViewers,
                                viewerCount = if (amHost) {
                                    updatedViewers.size
                                } else {
                                    _state.value.viewerCount
                                }
                            )
                            if (amHost) {
                                webRtcManager.addViewer(id)
                                sendEvent(
                                    STATUS_VIEWER_COUNT,
                                    payload = updatedViewers.size.toString()
                                )
                            }
                        }
                        STATUS_LEFT -> live.senderId.let { id ->
                            val updatedViewers = _state.value.viewers - id
                            val amHost = profileRepository.requireUserId() == hostId
                            _state.value = _state.value.copy(
                                viewers = updatedViewers,
                                viewerCount = if (amHost) {
                                    updatedViewers.size
                                } else {
                                    _state.value.viewerCount
                                }
                            )
                            webRtcManager.removePeer(id)
                            if (amHost) {
                                sendEvent(
                                    STATUS_VIEWER_COUNT,
                                    payload = updatedViewers.size.toString()
                                )
                            }
                        }
                        STATUS_ENDED -> {
                            _state.value = _state.value.copy(hasEnded = true)
                            messageRepository.updateLiveStatus(
                                groupId,
                                liveId,
                                STATUS_ENDED
                            )
                        }
                        STATUS_HEARTBEAT -> Unit
                        STATUS_VIEWER_COUNT -> {
                            _state.value = _state.value.copy(
                                viewerCount = live.payload?.toIntOrNull()
                                    ?.coerceAtLeast(0)
                                    ?: _state.value.viewerCount
                            )
                        }
                        STATUS_SIGNAL -> handleSignal(live)
                    }
                }
        }
    }

    fun enterRoom(isHost: Boolean) {
        if (isHost) {
            heartbeatJob?.cancel()
            heartbeatJob = viewModelScope.launch {
                while (true) {
                    sendEvent(STATUS_HEARTBEAT)
                    delay(HEARTBEAT_INTERVAL_MS)
                }
            }
        } else {
            _state.value = _state.value.copy(viewerCount = 1)
            sendEvent(STATUS_JOINED)
            watchdogJob?.cancel()
            watchdogJob = viewModelScope.launch {
                while (true) {
                    delay(HEARTBEAT_INTERVAL_MS)
                    if (System.currentTimeMillis() - lastHostHeartbeat > HOST_TIMEOUT_MS) {
                        _state.value = _state.value.copy(hasEnded = true)
                        messageRepository.updateLiveStatus(groupId, liveId, STATUS_ENDED)
                        break
                    }
                }
            }
        }
    }

    fun leaveRoom(isHost: Boolean) {
        if (isHost) endLive() else sendEvent(STATUS_LEFT)
    }

    fun endLive() {
        heartbeatJob?.cancel()
        _state.value = _state.value.copy(hasEnded = true)
        viewModelScope.launch {
            messageRepository.updateLiveStatus(
                groupId,
                liveId,
                STATUS_ENDED
            )
        }
        sendEvent(STATUS_ENDED)
    }

    override fun onCleared() {
        webRtcManager.release()
    }

    private fun sendSignal(signal: LiveSignal) {
        sendEvent(
            status = STATUS_SIGNAL,
            targetId = signal.targetId,
            payload = json.encodeToString(signal)
        )
    }

    private suspend fun handleSignal(live: ChatProtocol.GroupLiveEvent) {
        val me = profileRepository.requireProfile()
        if (live.targetId != me.id) return
        val senderId = live.senderId
        val signal = live.payload?.let {
            runCatching { json.decodeFromString<LiveSignal>(it) }.getOrNull()
        } ?: return
        when (signal.type) {
            "offer" -> signal.sdp?.let { webRtcManager.handleOffer(hostId, it) }
            "answer" -> signal.sdp?.let { webRtcManager.handleAnswer(senderId, it) }
            "ice" -> webRtcManager.handleIce(senderId, signal)
        }
    }

    private fun sendEvent(
        status: String,
        displayName: String? = null,
        targetId: String? = null,
        payload: String? = null
    ) {
        viewModelScope.launch {
            val me = profileRepository.requireProfile()
            liveEventBus.send(
                groupId = groupId,
                liveId = liveId,
                status = status,
                displayName = displayName ?: me.nickname,
                targetId = targetId,
                payload = payload
            )
        }
    }

    private companion object {
        const val STATUS_ENDED = "ended"
        const val STATUS_JOINED = "joined"
        const val STATUS_LEFT = "left"
        const val STATUS_HEARTBEAT = "heartbeat"
        const val STATUS_VIEWER_COUNT = "viewer_count"
        const val STATUS_SIGNAL = "signal"
        const val HEARTBEAT_INTERVAL_MS = 10_000L
        const val HOST_TIMEOUT_MS = 35_000L
    }
}
