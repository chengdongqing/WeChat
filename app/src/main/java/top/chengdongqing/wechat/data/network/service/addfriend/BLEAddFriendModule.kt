package top.chengdongqing.wechat.data.network.service.addfriend

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.toBytes
import top.chengdongqing.wechat.core.util.toMD5Bytes
import top.chengdongqing.wechat.data.network.ble.BLEConfig
import top.chengdongqing.wechat.data.network.ble.BLEPacket
import top.chengdongqing.wechat.data.network.ble.BLEPacketType
import top.chengdongqing.wechat.data.network.ble.BLEServer
import top.chengdongqing.wechat.data.network.ble.ServerPacketEvent
import top.chengdongqing.wechat.data.network.model.FriendEvent
import top.chengdongqing.wechat.data.network.model.FriendProtocol
import top.chengdongqing.wechat.data.network.service.ServiceModule
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.data.model.toBeacon
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Business logic for the "Add Friend via BLE" feature.
 *
 * All BLE mechanics are delegated to [BLEServer] (server role) and
 * [top.chengdongqing.wechat.data.network.ble.BLEConnectionManager] (client role).
 *
 * Responsibilities of this class:
 *  1. Start / stop advertising (delegate to [BLEServer])
 *  2. Push own profile to a newly-subscribed remote client
 *  3. Reassemble multi-chunk incoming messages per device
 *  4. Decode [FriendProtocol] and emit [FriendEvent]s
 */
@Singleton
class BLEAddFriendModule @Inject constructor(
    private val server: BLEServer,
    private val profileRepository: ProfileRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val json: Json,
    @param:IoScope private val scope: CoroutineScope,
) : ServiceModule {

    companion object {
        private const val TAG = "BLEAddFriendModule"
    }

    private val _friendEvents = MutableSharedFlow<FriendEvent>(extraBufferCapacity = 8)
    val friendEvents: SharedFlow<FriendEvent> = _friendEvents.asSharedFlow()

    /**
     * Per-device reassembly buffers keyed by MAC address.
     *
     * A device that connects and sends packets but never sends END will accumulate
     * data here until it disconnects (cleaned up via [BLEServer.disconnections]).
     */
    private val messageBuffers = ConcurrentHashMap<String, IncomingBuffer>()

    override fun start() {
        val userId = profileRepository.requireUserId()
        val userIdHash = userId.toMD5Bytes().copyOf(BLEConfig.USER_ID_HASH_LENGTH)
        server.start(userIdHash)
        observeServerEvents()

        Log.d(TAG, "加好友模块已启动")
    }

    override fun stop() {
        server.stop()
        messageBuffers.clear()

        Log.d(TAG, "加好友模块已停止")
    }

    private fun observeServerEvents() {
        // Reassemble incoming BlePacket chunks into complete FriendProtocol messages
        scope.launch {
            server.packets.collect { event -> handleIncomingPacket(event) }
        }

        // A remote client subscribed to notifications → push our profile to them
        scope.launch {
            server.subscriptions.collect { device -> sendProfileToDevice(device) }
        }

        // Remote client disconnected → discard its incomplete buffer
        scope.launch {
            server.disconnections.collect { device ->
                messageBuffers.remove(device.address)?.let {
                    Log.d(TAG, "已清除设备缓冲: ${device.address}")
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun handleIncomingPacket(event: ServerPacketEvent) {
        val (device, packet) = event
        val buffer = messageBuffers.getOrPut(device.address) { IncomingBuffer() }

        when (packet.type) {
            BLEPacketType.JSON -> buffer.jsonBuffer.write(packet.body)
            BLEPacketType.BINARY -> buffer.binaryBuffer.write(packet.body)
            BLEPacketType.END -> {
                messageBuffers.remove(device.address)
                processCompleteMessage(buffer)
            }

            else -> Log.w(TAG, "未知 packet type: ${packet.type}")
        }
    }

    private suspend fun processCompleteMessage(buffer: IncomingBuffer) {
        val jsonText = String(buffer.jsonBuffer.toByteArray(), Charsets.UTF_8)
        val message = runCatching {
            json.decodeFromString<FriendProtocol>(jsonText)
        }.getOrNull() ?: run {
            Log.e(TAG, "FriendProtocol 解析失败")
            return
        }
        val binary = buffer.binaryBuffer.toByteArray().takeIf { it.isNotEmpty() }

        when (message) {
            is FriendProtocol.FriendRequest -> handleFriendRequest(message, binary)
            is FriendProtocol.FriendResponse -> handleFriendResponse(message)
        }
    }

    private suspend fun handleFriendRequest(
        message: FriendProtocol.FriendRequest,
        binary: ByteArray?,
    ) {
        friendRequestRepository.handleIncomingRequest(message.toDomain(binary))
        _friendEvents.emit(FriendEvent.FriendRequest(message.nickname, message.greeting))
    }

    private suspend fun handleFriendResponse(message: FriendProtocol.FriendResponse) {
        friendRequestRepository.handleRequestResponse(message.toDomain())
        _friendEvents.emit(FriendEvent.FriendResponse(message.result))
    }

    private fun sendProfileToDevice(device: BluetoothDevice) {
        scope.launch {
            runCatching {
                val profile = profileRepository.requireProfile()
                val avatarBytes = generateAvatarThumbnail(profile.avatarPath)

                val jsonBytes = json.encodeToString(profile.toBeacon(avatarBytes))
                    .toByteArray(Charsets.UTF_8)

                // JSON chunks
                jsonBytes.forEachPacket(BLEPacketType.JSON) { packet ->
                    server.sendPacket(device, packet)
                }
                // Binary (avatar) chunks
                avatarBytes?.forEachPacket(BLEPacketType.BINARY) { packet ->
                    server.sendPacket(device, packet)
                }
                // Signal end of transfer
                server.sendPacket(device, BLEPacket.end())

                Log.d(TAG, "资料已推送: ${device.address}")
            }.onFailure {
                Log.e(TAG, "推送资料失败: ${device.address}", it)
            }
        }
    }

    private fun generateAvatarThumbnail(avatarPath: String?): ByteArray? =
        avatarPath?.let { path ->
            File(path).toBytes(
                targetSize = BLEConfig.AVATAR_THUMBNAIL_SIZE,
                maxSizeKB = BLEConfig.AVATAR_MAX_SIZE_KB,
            )
        }
}

/**
 * Iterates over [BLEConfig.MAX_PACKET_BODY]-byte chunks of this [ByteArray], wrapping each
 * in a [BLEPacket] of [type] and invoking [action] for each.
 */
private inline fun ByteArray.forEachPacket(
    type: Byte,
    action: (BLEPacket) -> Unit,
) {
    var offset = 0
    while (offset < size) {
        val end = minOf(offset + BLEConfig.MAX_PACKET_BODY, size)
        action(BLEPacket(type, copyOfRange(offset, end)))
        offset = end
    }
}

/**
 * Holds per-device JSON and binary reassembly buffers.
 * Discarded when the device sends END or disconnects.
 */
private class IncomingBuffer {
    val jsonBuffer = ByteArrayOutputStream()
    val binaryBuffer = ByteArrayOutputStream()
}