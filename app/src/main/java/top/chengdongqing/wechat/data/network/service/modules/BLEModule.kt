package top.chengdongqing.wechat.data.network.service.modules

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.core.util.toMD5Bytes
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.RequestAction
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.data.model.UserProfileTransfer
import top.chengdongqing.wechat.features.me.domain.model.Gender.Companion.getIndex
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE 模块 - 负责好友添加功能
 *
 * 主要功能:
 * 1. 启动 BLE 广播，向附近设备广播用户身份（MD5 哈希）
 * 2. 启动 GATT 服务器，接收和发送好友请求数据
 * 3. 管理设备会话，处理数据分片传输
 * 4. 支持两阶段数据传输：JSON 元数据 + 二进制数据（如头像）
 * 5. 支持碰一碰（NFC）流程的消息路由
 */
@Singleton
class BLEModule @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val imageExt: ImageExt,
    private val json: Json
) {

    companion object {
        private const val TAG = "BLEModule"

        val SERVICE_UUID: UUID = UUID.fromString("0000FE9F-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000FEA0-0000-1000-8000-00805F9B34FB")
        val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val MAX_CHUNK_SIZE = 500
        private const val CHUNK_DELAY_MS = 50L
        private const val PROFILE_SEND_DELAY_MS = 100L

        private const val USER_ID_HASH_LENGTH = 4

        private const val AVATAR_THUMBNAIL_SIZE = 100
        private const val AVATAR_MAX_SIZE_KB = 5

        private const val JSON_START_MARKER = '{'
        private const val JSON_END_MARKER = '}'
    }

    // 事件流：所有好友请求相关事件都从这里发出
    private val _friendRequestEvents =
        MutableSharedFlow<FriendRequestEvent>(extraBufferCapacity = 8)
    val friendRequestEvents = _friendRequestEvents.asSharedFlow()

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null

    // 设备会话：key = 设备MAC地址
    private val deviceSessions = ConcurrentHashMap<String, DeviceSession>()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    // ==================== 公共接口 ====================

    fun start(scope: CoroutineScope) {
        startBLEAdvertising(scope)
        startGattServer(scope)
        Log.d(TAG, "✅ BLE 模块已启动")
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
            gattServer?.close()
            deviceSessions.clear()
            Log.d(TAG, "BLE 模块已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止 BLE 服务失败", e)
        }
    }

    // ==================== BLE 广播 ====================

    @SuppressLint("MissingPermission")
    private fun startBLEAdvertising(scope: CoroutineScope) {
        scope.launch {
            try {
                val adapter = bluetoothAdapter ?: run {
                    Log.w(TAG, "蓝牙适配器不可用")
                    return@launch
                }
                if (!adapter.isEnabled) {
                    Log.w(TAG, "蓝牙未启用")
                    return@launch
                }

                bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser ?: run {
                    Log.w(TAG, "设备不支持 BLE 广播")
                    return@launch
                }

                val myProfile = profileRepository.getProfile() ?: run {
                    Log.w(TAG, "无法获取个人资料")
                    return@launch
                }
                val userIdHash = myProfile.id.toMD5Bytes().copyOf(USER_ID_HASH_LENGTH)

                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .build()

                val data = AdvertiseData.Builder()
                    .setIncludeDeviceName(false)
                    .addServiceUuid(ParcelUuid(SERVICE_UUID))
                    .addServiceData(ParcelUuid(SERVICE_UUID), userIdHash)
                    .build()

                bluetoothLeAdvertiser!!.startAdvertising(
                    settings, data,
                    object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                            Log.d(TAG, "✅ BLE 广播已启动")
                        }

                        override fun onStartFailure(errorCode: Int) {
                            Log.e(TAG, "BLE 广播启动失败，错误码: $errorCode")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "启动 BLE 广播异常", e)
            }
        }
    }

    // ==================== GATT 服务器 ====================

    @SuppressLint("MissingPermission")
    private fun startGattServer(scope: CoroutineScope) {
        try {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            val characteristic = BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
            ).apply {
                addDescriptor(
                    BluetoothGattDescriptor(
                        DESCRIPTOR_UUID,
                        BluetoothGattDescriptor.PERMISSION_READ or
                                BluetoothGattDescriptor.PERMISSION_WRITE
                    )
                )
            }

            val service = BluetoothGattService(
                SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            ).apply {
                addCharacteristic(characteristic)
            }

            gattServer = bluetoothManager.openGattServer(
                context,
                GattServerCallbackImpl(scope)
            )
            gattServer?.addService(service)
            Log.d(TAG, "✅ GATT 服务器已启动")
        } catch (e: Exception) {
            Log.e(TAG, "启动 GATT 服务器异常", e)
        }
    }

    // ==================== GATT 回调 ====================

    private inner class GattServerCallbackImpl(
        private val scope: CoroutineScope
    ) : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    deviceSessions[device.address] = DeviceSession()
                    Log.d(TAG, "✅ 设备已连接: ${device.address}")
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    deviceSessions.remove(device.address)
                    Log.d(TAG, "设备已断开: ${device.address}")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (descriptor.uuid == DESCRIPTOR_UUID) {
                Log.d(TAG, "客户端订阅了通知: ${device.address}")
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
                scope.launch { sendProfileData(device) }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            scope.launch {
                try {
                    val session = deviceSessions[device.address]
                    if (session == null) {
                        Log.w(TAG, "设备会话不存在: ${device.address}")
                        sendGattResponse(
                            device,
                            requestId,
                            responseNeeded,
                            BluetoothGatt.GATT_FAILURE
                        )
                        return@launch
                    }

                    if (!session.isReceivingBinary) {
                        handleJsonDataChunk(session, value)
                    } else {
                        handleBinaryDataChunk(session, value)
                    }

                    sendGattResponse(device, requestId, responseNeeded, BluetoothGatt.GATT_SUCCESS)
                } catch (e: Exception) {
                    Log.e(TAG, "处理写入请求失败: ${device.address}", e)
                    sendGattResponse(device, requestId, responseNeeded, BluetoothGatt.GATT_FAILURE)
                }
            }
        }

        // -------- 数据接收 --------

        private suspend fun handleJsonDataChunk(session: DeviceSession, chunk: ByteArray) {
            session.buffer.write(chunk)
            val jsonString = String(session.buffer.toByteArray(), Charsets.UTF_8)

            if (jsonString.startsWith(JSON_START_MARKER) && jsonString.endsWith(JSON_END_MARKER)) {
                val message = tryParseJsonMessage(jsonString) ?: return
                session.setCurrentMessage(message)
                session.buffer.reset()

                val binarySize = extractBinarySizeFromMessage(message)
                if (binarySize > 0) {
                    session.transitionToBinaryMode(binarySize)
                } else {
                    handleCompleteMessage(message, null)
                    session.reset()
                }
            }
        }

        private suspend fun handleBinaryDataChunk(session: DeviceSession, chunk: ByteArray) {
            session.buffer.write(chunk)
            if (session.buffer.size() >= session.expectedBinarySize) {
                val binaryData = session.buffer.toByteArray()
                handleCompleteMessage(session.currentMessage, binaryData)
                session.reset()
            }
        }

        // -------- 消息分发 --------

        private suspend fun handleCompleteMessage(message: P2PMessage?, binaryData: ByteArray?) {
            if (message == null) return
            try {
                when (message) {
                    is P2PMessage.FriendRequest -> handleFriendRequest(message, binaryData)
                    is P2PMessage.FriendRequestResponse -> handleFriendRequestResponse(message)
                    is P2PMessage.AutoAddResponse -> handleAutoAddResponse(message, binaryData)
                    is P2PMessage.FullProfileResponse -> handleFullProfileResponse(
                        message,
                        binaryData
                    )
                    // 碰一碰：收到对方的申请
                    is P2PMessage.NfcAddRequest -> handleNfcAddRequest(message, binaryData)
                    // 碰一碰：收到对方的确认响应
                    is P2PMessage.NfcAddResponse -> handleNfcAddResponse(message, binaryData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理消息失败: ${message::class.simpleName}", e)
            }
        }

        // -------- 各类消息处理器 --------

        private suspend fun handleFriendRequest(
            message: P2PMessage.FriendRequest,
            binaryData: ByteArray?
        ) {
            friendRequestRepository.handleIncomingRequest(message.toDomain(binaryData))
            _friendRequestEvents.emit(
                FriendRequestEvent.NewRequest(
                    nickname = message.peerNickname,
                    message = message.greetingMessage
                )
            )
            Log.d(TAG, "收到好友请求: ${message.peerNickname}")
        }

        private suspend fun handleFriendRequestResponse(message: P2PMessage.FriendRequestResponse) {
            friendRequestRepository.handleRequestResponse(message.toDomain())
            if (message.action == RequestAction.ACCEPT) {
                _friendRequestEvents.emit(
                    FriendRequestEvent.RequestAccepted(context.getString(R.string.contact_notification_request_accepted))
                )
                Log.d(TAG, "好友请求已被接受")
            }
        }

        private suspend fun handleAutoAddResponse(
            message: P2PMessage.AutoAddResponse,
            binaryData: ByteArray?
        ) {
            friendRequestRepository.handleAutoAddResponse(message.toDomain(binaryData))
            _friendRequestEvents.emit(FriendRequestEvent.AutoAdded(message.nickname))
            Log.d(TAG, "自动添加好友: ${message.nickname}")
        }

        private suspend fun handleFullProfileResponse(
            message: P2PMessage.FullProfileResponse,
            binaryData: ByteArray?
        ) {
            friendRequestRepository.handleFullProfileResponse(message.toDomain(binaryData))
            Log.d(TAG, "收到完整资料响应")
        }

        /**
         * 碰一碰：收到对方的 NfcAddRequest
         * 直接透传到事件流，由 NfcAddFriendViewModel 处理业务逻辑
         */
        private suspend fun handleNfcAddRequest(
            message: P2PMessage.NfcAddRequest,
            binaryData: ByteArray?
        ) {
            Log.d(TAG, "收到碰一碰好友申请: ${message.nickname}")
            _friendRequestEvents.emit(
                FriendRequestEvent.NfcPeerAddRequest(message, binaryData)
            )
        }

        /**
         * 碰一碰：收到对方的 NfcAddResponse（对方确认了我的申请）
         * 直接透传到事件流，由 NfcAddFriendViewModel 处理业务逻辑
         */
        private suspend fun handleNfcAddResponse(
            message: P2PMessage.NfcAddResponse,
            binaryData: ByteArray?
        ) {
            Log.d(TAG, "收到碰一碰好友响应: ${message.nickname}")
            _friendRequestEvents.emit(
                FriendRequestEvent.NfcPeerAddResponse(message, binaryData)
            )
        }

        // -------- 工具方法 --------

        @SuppressLint("MissingPermission")
        private fun sendGattResponse(
            device: BluetoothDevice,
            requestId: Int,
            responseNeeded: Boolean,
            status: Int
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, status, 0, null)
            }
        }

        private fun tryParseJsonMessage(jsonString: String): P2PMessage? {
            return try {
                json.decodeFromString<P2PMessage>(jsonString)
            } catch (e: Exception) {
                Log.w(TAG, "JSON 解析失败: ${e.message}")
                null
            }
        }

        private fun extractBinarySizeFromMessage(message: P2PMessage): Int {
            return when (message) {
                is P2PMessage.FriendRequest -> message.avatarSize
                is P2PMessage.AutoAddResponse -> message.avatarSize
                is P2PMessage.FullProfileResponse -> message.avatarSize
                is P2PMessage.NfcAddRequest -> message.avatarSize
                is P2PMessage.NfcAddResponse -> message.avatarSize
                else -> 0
            }
        }

        // ==================== 主动发送资料 ====================

        /**
         * 客户端订阅 Notification 后，主动推送本方资料给对方（BLE雷达场景）
         */
        @SuppressLint("MissingPermission")
        private suspend fun sendProfileData(device: BluetoothDevice) {
            try {
                val myProfile = profileRepository.observeProfile().first() ?: run {
                    Log.w(TAG, "无法获取个人资料")
                    return
                }

                val avatarBytes = generateAvatarThumbnail(myProfile.avatarPath)

                val profileTransfer = UserProfileTransfer(
                    userId = myProfile.id,
                    nickname = myProfile.nickname,
                    signature = myProfile.signature,
                    gender = myProfile.gender.getIndex(),
                    avatarSize = avatarBytes?.size ?: 0
                )

                val characteristic = getCharacteristic() ?: run {
                    Log.w(TAG, "GATT 特征值不可用")
                    return
                }
                val server = gattServer ?: return

                val jsonBytes = json.encodeToString(profileTransfer).toByteArray(Charsets.UTF_8)
                sendDataChunked(server, device, characteristic, jsonBytes, "ProfileJSON")

                if (avatarBytes != null) {
                    delay(PROFILE_SEND_DELAY_MS)
                    sendDataChunked(server, device, characteristic, avatarBytes, "Avatar")
                }

                Log.d(TAG, "资料发送完成: ${device.address}")
            } catch (e: Exception) {
                Log.e(TAG, "发送资料失败: ${device.address}", e)
            }
        }

        private fun generateAvatarThumbnail(avatarPath: String?): ByteArray? {
            return avatarPath?.let { path ->
                try {
                    imageExt.generateThumbnailBytes(
                        path,
                        targetSize = AVATAR_THUMBNAIL_SIZE,
                        maxSizeKB = AVATAR_MAX_SIZE_KB
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "生成头像缩略图失败", e)
                    null
                }
            }
        }

        private fun getCharacteristic(): BluetoothGattCharacteristic? {
            return gattServer?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)
        }

        @SuppressLint("MissingPermission")
        private suspend fun sendDataChunked(
            server: BluetoothGattServer,
            device: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic,
            data: ByteArray,
            dataType: String
        ) {
            var offset = 0
            var chunkCount = 0

            while (offset < data.size) {
                val chunkSize = minOf(MAX_CHUNK_SIZE, data.size - offset)
                val chunk = data.copyOfRange(offset, offset + chunkSize)

                val success =
                    notifyCharacteristicChangedCompat(server, device, characteristic, value = chunk)
                if (!success) {
                    Log.w(TAG, "[$dataType] 发送失败，已发送 $chunkCount 片")
                    break
                }

                offset += chunkSize
                chunkCount++

                if (offset < data.size) delay(CHUNK_DELAY_MS)
            }

            Log.d(TAG, "[$dataType] 发送完成: $chunkCount 片，总大小 ${data.size} 字节")
        }

        @SuppressLint("MissingPermission")
        private fun notifyCharacteristicChangedCompat(
            server: BluetoothGattServer,
            device: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic,
            confirm: Boolean = false,
            value: ByteArray
        ): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    server.notifyCharacteristicChanged(
                        device, characteristic, confirm, value
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = value
                    @Suppress("DEPRECATION")
                    server.notifyCharacteristicChanged(device, characteristic, confirm)
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送通知失败", e)
                false
            }
        }
    }

    // ==================== 设备会话 ====================

    private class DeviceSession {
        var isReceivingBinary: Boolean = false
            private set

        val buffer: ByteArrayOutputStream = ByteArrayOutputStream()

        var currentMessage: P2PMessage? = null
            private set

        var expectedBinarySize: Int = 0
            private set

        fun transitionToBinaryMode(binarySize: Int) {
            isReceivingBinary = true
            expectedBinarySize = binarySize
        }

        fun reset() {
            buffer.reset()
            currentMessage = null
            expectedBinarySize = 0
            isReceivingBinary = false
        }

        fun setCurrentMessage(message: P2PMessage) {
            currentMessage = message
        }
    }
}

/**
 * 好友请求事件
 *
 * 用于向 UI 层传递好友请求相关的事件
 */
sealed class FriendRequestEvent {
    /**
     * 扫一扫流程：收到新的好友请求
     */
    data class NewRequest(
        val nickname: String,
        val message: String
    ) : FriendRequestEvent()

    /**
     * 扫一扫流程：好友请求被接受
     */
    data class RequestAccepted(
        val message: String
    ) : FriendRequestEvent()

    /**
     * 自动添加成功（删除后重新添加场景）
     */
    data class AutoAdded(
        val nickname: String
    ) : FriendRequestEvent()

    /**
     * 碰一碰流程：收到对方发来的 NfcAddRequest
     *
     * 表示对方已点击"添加到通讯录"，等待本方操作：
     * - 若本方已点击：直接存库并回复 NfcAddResponse
     * - 若本方未点击：展示"对方已准备好"提示
     */
    data class NfcPeerAddRequest(
        val message: P2PMessage.NfcAddRequest,
        val avatarBytes: ByteArray?
    ) : FriendRequestEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NfcPeerAddRequest

            if (message != other.message) return false
            if (!avatarBytes.contentEquals(other.avatarBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
            return result
        }
    }

    /**
     * 碰一碰流程：收到对方发来的 NfcAddResponse
     *
     * 表示对方确认了本方的添加请求，双方交换完成。
     * 收到此事件后将对方资料存库并显示成功。
     */
    data class NfcPeerAddResponse(
        val message: P2PMessage.NfcAddResponse,
        val avatarBytes: ByteArray?
    ) : FriendRequestEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NfcPeerAddResponse

            if (message != other.message) return false
            if (!avatarBytes.contentEquals(other.avatarBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
            return result
        }
    }
}