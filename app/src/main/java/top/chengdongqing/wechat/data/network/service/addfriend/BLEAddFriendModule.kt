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
 * 基于 BLE 蓝牙的加好友模块
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
     * 各设备的分片重组缓冲区，以 MAC 地址为 key。
     *
     * 若设备连接后持续发包但始终未发 END，数据将一直积压在此，
     * 直到设备断开连接时由 [BLEServer.disconnections] 触发清理。
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
            // 将收到的 BLEPacket 分片重组为完整的 FriendProtocol 消息
            launch {
                server.packets.collect { event -> handleIncomingPacket(event) }
            }
            // 远端客户端订阅通知时，主动推送本机资料
            launch {
                server.subscriptions.collect { device -> sendProfileToDevice(device) }
            }
            // 远端客户端断开连接时，丢弃其未完成的缓冲区
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
        friendRequestRepository.handleIncomingRequest(message, binary)

        // 判断对方是否已在通讯录中
        val exists = contactRepository.exists(message.userId)

        // 满足以下两个条件时推送通知：1. 开启了好友验证；2. 对方不在通讯录
        if (!exists && friendVerifyEnabled()) {
            _friendEvents.emit(FriendEvent.FriendRequest(message.nickname, message.greeting))
        }
    }

    private suspend fun handleFriendResponse(message: FriendProtocol.FriendResponse) {
        friendRequestRepository.handleRequestResponse(message)
    }

    private fun sendProfileToDevice(device: BluetoothDevice) {
        scope.launch {
            runCatching {
                val profile = profileRepository.requireProfile()
                val avatarBytes = generateAvatarThumbnail(profile.avatarPath)

                val jsonBytes = json.encodeToString(profile.toBeacon(avatarBytes))
                    .toByteArray(Charsets.UTF_8)

                // 发送 JSON 分片
                jsonBytes.forEachPacket(BLEPacketType.JSON) { packet ->
                    server.sendPacket(device, packet)
                }
                // 发送头像二进制分片
                avatarBytes?.forEachPacket(BLEPacketType.BINARY) { packet ->
                    server.sendPacket(device, packet)
                }
                // 发送结束标志
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
 * 将 [ByteArray] 按 [BLEConfig.MAX_PACKET_BODY] 大小分片，
 * 每片封装为指定 [type] 的 [BLEPacket] 后通过 [action] 回调。
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
 * 单个设备的消息重组缓冲区，分别存储 JSON 和二进制数据。
 * 设备发送 END 包或断开连接时丢弃。
 */
private class IncomingBuffer {
    val jsonBuffer = ByteArrayOutputStream()
    val binaryBuffer = ByteArrayOutputStream()
}