package top.chengdongqing.wechat.core.network.messaging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.connection.ConnectionManager
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.runtime.IoScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class RealtimePacket(
    val senderId: String,
    val type: Byte,
    val body: ByteArray
)

/** Ephemeral encrypted packets routed over the currently selected transport. */
@Singleton
class RealtimePacketBus @Inject constructor(
    private val transport: ChatTransportManager,
    private val connections: ConnectionManager,
    @IoScope private val scope: CoroutineScope
) {
    private val _events = MutableSharedFlow<RealtimePacket>(extraBufferCapacity = 256)
    val events = _events.asSharedFlow()
    private val recentlySeen = ConcurrentHashMap<Int, Long>()

    suspend fun broadcast(type: Byte, body: ByteArray): Int {
        markSeen(type, body)
        val targets = connections.connections.keys.toList()
        return targets.count {
            transport.send(it, Packet(type, body)).isSuccess
        }
    }

    internal fun receive(senderId: String, packet: Packet) {
        if (!markSeen(packet.type, packet.body)) return
        _events.tryEmit(RealtimePacket(senderId, packet.type, packet.body))
        // Wi-Fi Direct and Bluetooth commonly form a star. The group owner/
        // center relays an ephemeral frame to its other directly connected peers.
        val relayTargets = connections.connections.keys.filter { it != senderId }
        if (relayTargets.isNotEmpty()) {
            scope.launch {
                relayTargets.forEach { transport.send(it, packet) }
            }
        }
    }

    private fun markSeen(type: Byte, body: ByteArray): Boolean {
        val now = System.currentTimeMillis()
        val key = 31 * type + body.contentHashCode()
        val previous = recentlySeen.put(key, now)
        if (recentlySeen.size > 2_048) {
            recentlySeen.entries.removeIf { now - it.value > DEDUP_WINDOW_MS }
        }
        return previous == null || now - previous > DEDUP_WINDOW_MS
    }

    private companion object {
        const val DEDUP_WINDOW_MS = 5_000L
    }
}
