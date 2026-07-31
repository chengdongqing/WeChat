package top.chengdongqing.wechat.feature.chat.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.network.audio.IntercomAudioEngine
import top.chengdongqing.wechat.core.network.audio.IntercomTransport
import top.chengdongqing.wechat.core.network.messaging.RealtimePacketBus
import top.chengdongqing.wechat.core.network.model.PacketType

@HiltViewModel(assistedFactory = LiveLocationViewModel.Factory::class)
class LiveLocationViewModel @AssistedInject constructor(
    @Assisted val chatId: String,
    private val packets: RealtimePacketBus,
    private val json: Json,
    profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
    private val registry: LiveLocationSessionRegistry,
    val audio: IntercomAudioEngine
) : ViewModel() {
    val myUserId = profileRepository.requireUserId()
    val myAvatar = profileRepository.getProfile()?.avatarPath
    private val myName = profileRepository.getProfile()?.nickname.orEmpty()
    val roomId = registry.roomIdFor(chatId)
    val roomState = registry.rooms.map {
        it[roomId] ?: LiveLocationRoomState(roomId)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        registry.room(roomId)
    )
    private val _remoteLocation = MutableStateFlow<GeoPoint?>(null)
    val remoteLocation = _remoteLocation.asStateFlow()
    private val _remoteBearing = MutableStateFlow<Float?>(null)
    val remoteBearing = _remoteBearing.asStateFlow()
    private val _peerPresent = MutableStateFlow(false)
    val peerPresent = _peerPresent.asStateFlow()
    private var currentLocation: GeoPoint? = null
    private var currentBearing: Float? = null
    private var lastSentAt = 0L
    private var lastBearingSentAt = 0L
    private var heartbeatJob: Job? = null
    private var lastPeerUpdateAt = 0L
    private val _participants = MutableStateFlow<List<LiveLocationParticipantUi>>(emptyList())
    val participants = _participants.asStateFlow()

    init {
        viewModelScope.launch {
            packets.events.filter { it.type == PacketType.LIVE_LOCATION }.collect { event ->
                val update = runCatching {
                    json.decodeFromString<LiveLocationWireUpdate>(event.body.decodeToString())
                }.getOrNull() ?: return@collect
                if (update.roomId == roomId && update.active) {
                    lastPeerUpdateAt = System.currentTimeMillis()
                    _peerPresent.value = true
                    if (update.latitude != null && update.longitude != null) {
                        _remoteLocation.value = GeoPoint(update.latitude, update.longitude)
                        _remoteBearing.value = update.bearing
                    }
                } else if (update.roomId == roomId) {
                    _peerPresent.value = false
                    _remoteLocation.value = null
                    _remoteBearing.value = null
                }
            }
        }
        viewModelScope.launch {
            roomState.collect { room ->
                _participants.value = room.participants.keys.map { id ->
                    if (id == myUserId) {
                        LiveLocationParticipantUi(id, myName, myAvatar)
                    } else {
                        val contact = contactRepository.getContact(id)
                        LiveLocationParticipantUi(
                            id,
                            contact?.displayName ?: id,
                            contact?.avatarPath
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(PEER_TIMEOUT_CHECK_MS)
                if (_peerPresent.value &&
                    System.currentTimeMillis() - lastPeerUpdateAt > PEER_TIMEOUT_MS
                ) {
                    _peerPresent.value = false
                    _remoteLocation.value = null
                    _remoteBearing.value = null
                }
            }
        }
    }

    fun start() {
        audio.start(roomId, transport = IntercomTransport.PrivateChat)
        if (heartbeatJob?.isActive == true) return
        broadcastActive()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                broadcastActive()
            }
        }
    }

    fun updateLocation(point: GeoPoint, bearing: Float? = null) {
        val now = System.currentTimeMillis()
        currentLocation = point
        currentBearing = bearing ?: currentBearing
        if (now - lastSentAt < LOCATION_INTERVAL_MS) return
        lastSentAt = now
        broadcastActive()
    }

    fun updateBearing(bearing: Float) {
        currentBearing = bearing
        val now = System.currentTimeMillis()
        if (currentLocation != null && now - lastBearingSentAt >= BEARING_INTERVAL_MS) {
            lastBearingSentAt = now
            broadcastActive()
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        send(LiveLocationWireUpdate(roomId, active = false))
        registry.leaveLocal(roomId)
        audio.stop()
    }

    private fun broadcastActive() {
        val point = currentLocation
        send(
            LiveLocationWireUpdate(
                roomId = roomId,
                latitude = point?.latitude,
                longitude = point?.longitude,
                bearing = currentBearing,
                active = true
            )
        )
    }

    private fun send(update: LiveLocationWireUpdate) {
        registry.joinLocal(update)
        viewModelScope.launch {
            packets.broadcast(
                PacketType.LIVE_LOCATION,
                json.encodeToString(update).encodeToByteArray()
            )
        }
    }

    override fun onCleared() {
        audio.stop()
    }

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): LiveLocationViewModel
    }

    private companion object {
        const val LOCATION_INTERVAL_MS = 1_000L
        const val BEARING_INTERVAL_MS = 200L
        const val HEARTBEAT_INTERVAL_MS = 2_000L
        const val PEER_TIMEOUT_CHECK_MS = 1_000L
        const val PEER_TIMEOUT_MS = 6_000L
    }
}

data class LiveLocationParticipantUi(
    val userId: String,
    val name: String,
    val avatarPath: String?
)
