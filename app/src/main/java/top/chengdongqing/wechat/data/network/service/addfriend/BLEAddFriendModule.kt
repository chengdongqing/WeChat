package top.chengdongqing.wechat.data.network.service.addfriend

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
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
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.data.model.toBeacon
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import top.chengdongqing.wechat.features.settings.domain.repository.PrivacySettingsRepository
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
    private val json: Json,
    private val server: BLEServer,
    private val profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val privacySettingsRepository: PrivacySettingsRepository,
    @param:IoScope private val scope: CoroutineScope
) : ServiceModule {

    companion object {
        private const val TAG = "BLEAddFriendModule"
    }

    private val _friendEvents = MutableSharedFlow<FriendEvent>()
    val friendEvents: Flow<FriendEvent> = _friendEvents.asSharedFlow()

    /**
     * Per-device reassembly buffers keyed by MAC address.
     *
     * A device that connects and sends packets but never sends END will accumulate
     * data here until it disconnects (cleaned up via [BLEServer.disconnections]).
     */
    private val messageBuffers = ConcurrentHashMap<String, IncomingBuffer>()

    private var observerJob: Job? = null

    override fun start() {
        runCatching {
            val userId = profileRepository.requireUserId()
            val userIdHash = userId.toMD5Bytes().copyOf(BLEConfig.USER_ID_HASH_LENGTH)
            server.start(userIdHash)
            observeServerEvents()
        }.onSuccess {
            Log.d(TAG, "加好友模块已启动")
        }.onFailure {
            Log.d(TAG, "加好友模块启动失败", it)
        }
    }

    override fun stop() {
        runCatching {
            server.stop()
            messageBuffers.clear()
            observerJob?.cancel()
        }.onSuccess {
            Log.d(TAG, "加好友模块已停止")
        }
    }

    private fun observeServerEvents() {
        observerJob = scope.launch {
            // Reassemble incoming BlePacket chunks into complete FriendProtocol messages
            launch {
                server.packets.collect { event -> handleIncomingPacket(event) }
            }
            // A remote client subscribed to notifications → push our profile to them
            launch {
                server.subscriptions.collect { device -> sendProfileToDevice(device) }
            }
            // Remote client disconnected → discard its incomplete buffer
            launch {
                server.disconnections.collect { device ->
                    messageBuffers.remove(device.address)
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
        }.onFailure {
            Log.e(TAG, "FriendProtocol 解析失败", it)
        }.getOrNull() ?: return
        val binary = buffer.binaryBuffer.toByteArray().takeIf {
            it.isNotEmpty()
        }

        when (message) {
            is FriendProtocol.FriendRequest -> handleFriendRequest(message, binary)
            is FriendProtocol.FriendResponse -> handleFriendResponse(message)
        }
    }

    private suspend fun handleFriendRequest(
        message: FriendProtocol.FriendRequest,
        binary: ByteArray?,
    ) {
        friendRequestRepository.handleIncomingRequest(message, binary)

        // 查询该用户是否已是好友
        val exists = contactRepository.exists(message.userId)

        // 发送通知的情况：1.开启了好友验证 2.不在通讯录
        if (!exists && friendVerifyEnabled()) {
            _friendEvents.emit(FriendEvent.FriendRequest(message.nickname, message.greeting))
        }
    }

    private suspend fun handleFriendResponse(message: FriendProtocol.FriendResponse) {
        friendRequestRepository.handleRequestResponse(message)

        // 发送通知
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

    private suspend fun friendVerifyEnabled(): Boolean =
        privacySettingsRepository.friendVerifyEnabled.first()
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