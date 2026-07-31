package top.chengdongqing.wechat.feature.discovery.intercom

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.network.messaging.RealtimePacketBus
import top.chengdongqing.wechat.core.network.model.PacketType
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

data class NearbyIntercomChannel(
    val id: String,
    val name: String,
    val memberCount: Int,
    val speakingCount: Int,
    val lastSeenAt: Long
)

data class IntercomMember(
    val id: String,
    val nickname: String,
    val isSpeaking: Boolean,
    val isMe: Boolean
)

data class IntercomRoomState(
    val channelId: String = "",
    val channelName: String = "",
    val members: List<IntercomMember> = emptyList()
) {
    val speakers: List<IntercomMember>
        get() = members.filter(IntercomMember::isSpeaking)
}

@Serializable
private data class IntercomBeacon(
    val protocol: Int = 1,
    val channelId: String,
    val channelName: String,
    val deviceId: String,
    val nickname: String,
    val isSpeaking: Boolean,
    val sentAt: Long
)

private data class SeenMember(
    val beacon: IntercomBeacon,
    val receivedAt: Long
)

/**
 * Discovers intercom channels on the current LAN and advertises the channel
 * joined by this device. Audio transport intentionally lives separately.
 */
@Singleton
class IntercomLanDiscovery @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val json: Json,
    private val realtimePackets: RealtimePacketBus
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _channels = MutableStateFlow<List<NearbyIntercomChannel>>(emptyList())
    val channels = _channels.asStateFlow()
    private val _roomState = MutableStateFlow(IntercomRoomState())
    val roomState = _roomState.asStateFlow()

    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var realtimeJob: Job? = null
    private var heartbeatJob: Job? = null
    private var cleanupJob: Job? = null
    private val members = mutableMapOf<String, SeenMember>()

    @Volatile
    private var joinedChannel: Pair<String, String>? = null
    @Volatile
    private var speaking = false
    @Volatile
    private var mode = ConnectionMode.WiFiLan

    @Synchronized
    fun start(connectionMode: ConnectionMode = ConnectionMode.WiFiLan) {
        if (heartbeatJob?.isActive == true && mode == connectionMode) return
        stop()
        mode = connectionMode
        if (mode == ConnectionMode.WiFiLan) {
            val receiver = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                soTimeout = 1_000
                bind(InetSocketAddress(PORT))
            }
            socket = receiver
            receiveJob = scope.launch { receiveLoop(receiver) }
        } else {
            realtimeJob = scope.launch {
                realtimePackets.events
                    .filter { it.type == PacketType.INTERCOM_BEACON }
                    .collect { event ->
                        decodeAndRecordBeacon(event.body, event.body.size)
                    }
            }
        }
        heartbeatJob = scope.launch {
            while (isActive) {
                advertise()
                delay(HEARTBEAT_MS)
            }
        }
        cleanupJob = scope.launch {
            while (isActive) {
                delay(1_000)
                rebuildChannels()
            }
        }
    }

    @Synchronized
    fun stop() {
        joinedChannel = null
        speaking = false
        socket?.close()
        socket = null
        receiveJob?.cancel()
        receiveJob = null
        realtimeJob?.cancel()
        realtimeJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        cleanupJob?.cancel()
        cleanupJob = null
        synchronized(members) { members.clear() }
        _channels.value = emptyList()
        _roomState.value = IntercomRoomState()
    }

    fun join(channelId: String, channelName: String? = null) {
        val resolvedName = channelName
            ?.takeIf(String::isNotBlank)
            ?: joinedChannel?.takeIf { it.first == channelId }?.second
            ?: _channels.value.firstOrNull { it.id == channelId }?.name
            ?: "频道 $channelId"
        joinedChannel = channelId to resolvedName
        advertise()
    }

    fun leave() {
        joinedChannel = null
        speaking = false
        _roomState.value = IntercomRoomState()
        rebuildChannels()
    }

    fun setSpeaking(value: Boolean) {
        speaking = value
        advertise()
    }

    private fun receiveLoop(receiver: DatagramSocket) {
        val buffer = ByteArray(MAX_PACKET_SIZE)
        while (scope.isActive && !receiver.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                receiver.receive(packet)
                decodeAndRecordBeacon(packet.data, packet.length)
            } catch (_: SocketTimeoutException) {
                // Periodically wake so shutdown and cleanup remain responsive.
            } catch (error: Exception) {
                if (!receiver.isClosed) Log.w(TAG, "接收频道心跳失败", error)
            }
        }
    }

    private fun advertise() {
        val (channelId, channelName) = joinedChannel ?: return
        scope.launch {
            runCatching {
                val profile = profileRepository.requireProfile()
                val beacon = IntercomBeacon(
                    channelId = channelId,
                    channelName = channelName,
                    deviceId = profile.id,
                    nickname = profile.nickname,
                    isSpeaking = speaking,
                    sentAt = System.currentTimeMillis()
                )
                val bytes = json.encodeToString(beacon).encodeToByteArray()
                if (bytes.size > MAX_PACKET_SIZE) return@runCatching
                if (mode == ConnectionMode.WiFiLan) {
                    DatagramSocket().use { sender ->
                        sender.broadcast = true
                        sender.send(
                            DatagramPacket(
                                bytes,
                                bytes.size,
                                InetAddress.getByName(BROADCAST_ADDRESS),
                                PORT
                            )
                        )
                    }
                } else {
                    realtimePackets.broadcast(PacketType.INTERCOM_BEACON, bytes)
                }
                synchronized(members) {
                    members["$channelId:${profile.id}"] =
                        SeenMember(beacon, System.currentTimeMillis())
                }
                rebuildChannels()
            }.onFailure { Log.w(TAG, "发送频道心跳失败", it) }
        }
    }

    private fun decodeAndRecordBeacon(bytes: ByteArray, length: Int) {
        val beacon = json.decodeFromString<IntercomBeacon>(
            bytes.decodeToString(0, length)
        )
        if (beacon.protocol != PROTOCOL_VERSION) return
        val key = "${beacon.channelId}:${beacon.deviceId}"
        synchronized(members) {
            members[key] = SeenMember(beacon, System.currentTimeMillis())
        }
        rebuildChannels()
    }

    private fun rebuildChannels() {
        val now = System.currentTimeMillis()
        val snapshot = synchronized(members) {
            members.entries.removeAll { now - it.value.receivedAt > MEMBER_TIMEOUT_MS }
            members.values.toList()
        }
        _channels.value = snapshot
            .groupBy { it.beacon.channelId }
            .map { (channelId, seen) ->
                val newest = seen.maxBy { it.receivedAt }
                NearbyIntercomChannel(
                    id = channelId,
                    name = newest.beacon.channelName,
                    memberCount = seen.distinctBy { it.beacon.deviceId }.size,
                    speakingCount = seen.count { it.beacon.isSpeaking },
                    lastSeenAt = newest.receivedAt
                )
            }
            .sortedByDescending { it.lastSeenAt }
        val active = joinedChannel
        if (active != null) {
            val myId = runCatching { profileRepository.requireUserId() }.getOrNull()
            _roomState.value = IntercomRoomState(
                channelId = active.first,
                channelName = active.second,
                members = snapshot
                    .filter { it.beacon.channelId == active.first }
                    .distinctBy { it.beacon.deviceId }
                    .map {
                        IntercomMember(
                            id = it.beacon.deviceId,
                            nickname = it.beacon.nickname,
                            isSpeaking = it.beacon.isSpeaking,
                            isMe = it.beacon.deviceId == myId
                        )
                    }
                    .sortedWith(
                        compareByDescending<IntercomMember> { it.isSpeaking }
                            .thenByDescending { it.isMe }
                            .thenBy { it.nickname }
                    )
            )
        }
    }

    private companion object {
        const val TAG = "IntercomDiscovery"
        const val PROTOCOL_VERSION = 1
        const val PORT = 52_140
        const val BROADCAST_ADDRESS = "255.255.255.255"
        const val MAX_PACKET_SIZE = 2_048
        const val HEARTBEAT_MS = 1_500L
        const val MEMBER_TIMEOUT_MS = 5_000L
    }
}
