package top.chengdongqing.wechat.feature.chat.ui.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.network.messaging.RealtimePacketBus
import top.chengdongqing.wechat.core.network.model.PacketType
import javax.inject.Inject
import javax.inject.Singleton

data class LiveLocationParticipant(
    val userId: String,
    val location: GeoPoint?,
    val bearing: Float?,
    val lastSeenAt: Long
)

data class LiveLocationRoomState(
    val roomId: String,
    val participants: Map<String, LiveLocationParticipant> = emptyMap()
) {
    val isActive: Boolean get() = participants.isNotEmpty()
}

@Serializable
data class LiveLocationWireUpdate(
    val roomId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bearing: Float? = null,
    val active: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class LiveLocationSessionRegistry @Inject constructor(
    packets: RealtimePacketBus,
    private val profileRepository: ProfileRepository,
    private val json: Json,
    @IoScope scope: CoroutineScope
) {
    private val _rooms = MutableStateFlow<Map<String, LiveLocationRoomState>>(emptyMap())
    val rooms = _rooms.asStateFlow()

    init {
        scope.launch {
            packets.events.collect { event ->
                if (event.type != PacketType.LIVE_LOCATION) return@collect
                val update = runCatching {
                    json.decodeFromString<LiveLocationWireUpdate>(event.body.decodeToString())
                }.getOrNull() ?: return@collect
                applyUpdate(event.senderId, update)
            }
        }
        scope.launch {
            while (true) {
                delay(2_000)
                expireStaleParticipants()
            }
        }
    }

    fun roomIdFor(chatId: String): String =
        if (chatId.startsWith("group_")) chatId
        else listOf(profileRepository.requireUserId(), chatId).sorted().joinToString(":")

    fun room(roomId: String): LiveLocationRoomState =
        _rooms.value[roomId] ?: LiveLocationRoomState(roomId)

    fun joinLocal(update: LiveLocationWireUpdate) {
        applyUpdate(profileRepository.requireUserId(), update)
    }

    fun leaveLocal(roomId: String) {
        applyUpdate(
            profileRepository.requireUserId(),
            LiveLocationWireUpdate(roomId, active = false)
        )
    }

    private fun applyUpdate(userId: String, update: LiveLocationWireUpdate) {
        val rooms = _rooms.value.toMutableMap()
        val room = rooms[update.roomId] ?: LiveLocationRoomState(update.roomId)
        val participants = room.participants.toMutableMap()
        if (update.active) {
            participants[userId] = LiveLocationParticipant(
                userId = userId,
                location = if (update.latitude != null && update.longitude != null) {
                    GeoPoint(update.latitude, update.longitude)
                } else participants[userId]?.location,
                bearing = update.bearing ?: participants[userId]?.bearing,
                lastSeenAt = System.currentTimeMillis()
            )
        } else {
            participants.remove(userId)
        }
        if (participants.isEmpty()) rooms.remove(update.roomId)
        else rooms[update.roomId] = room.copy(participants = participants)
        _rooms.value = rooms
    }

    private fun expireStaleParticipants() {
        val now = System.currentTimeMillis()
        _rooms.value = _rooms.value.mapNotNull { (id, room) ->
            val active = room.participants.filterValues {
                now - it.lastSeenAt <= PARTICIPANT_TIMEOUT_MS
            }
            if (active.isEmpty()) null else id to room.copy(participants = active)
        }.toMap()
    }

    private companion object {
        const val PARTICIPANT_TIMEOUT_MS = 7_000L
    }
}
