package top.chengdongqing.wechat.data.network.service.addfriend

import android.annotation.SuppressLint
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.core.util.toMD5Bytes
import top.chengdongqing.wechat.data.network.model.FriendEvent
import top.chengdongqing.wechat.data.network.model.FriendProtocol
import top.chengdongqing.wechat.data.network.service.ServiceModule
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.data.model.UserProfileBeacon
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于BLE的加好友相关服务
 *
 * 启动 BLE 广播，向附近设备广播用户身份
 * 启动 GATT 服务器，接收和发送好友请求数据
 */
@Singleton
@SuppressLint("MissingPermission")
class BLEAddFriendModule @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val imageExt: ImageExt,
    private val json: Json,
    @param:IoScope private val scope: CoroutineScope,
    @param:ApplicationContext private val context: Context,
) : ServiceModule {
    companion object {
        private const val TAG = "BLEAddFriendModule"

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

    // 加好友相关事件流
    private val _friendEvents = MutableSharedFlow<FriendEvent>(extraBufferCapacity = 8)
    val friendEvents = _friendEvents.asSharedFlow()

    // 会话（key为MAC地址）
    private val deviceSessions = ConcurrentHashMap<String, DeviceSession>()

    private val bluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null

    override fun start() {
        runCatching {
            startBLEAdvertising()
            startGattServer()
        }.onSuccess {
            Log.d(TAG, "加好友模块已启动")
        }.onFailure {
            Log.e(TAG, "加好友模块启动失败", it)
        }
    }

    override fun stop() {
        runCatching {
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
            gattServer?.close()
            deviceSessions.clear()
        }.onSuccess {
            Log.d(TAG, "加好友模块已停止")
        }
    }

    /**
     * 启动BLE广播
     *
     * 方便随时接收加好友请求
     */
    private fun startBLEAdvertising() {
        scope.launch {
            runCatching {
                // 检查蓝牙状态
                if (checkBleReady().isFailure) return@launch
                // 获取 userId 哈希
                val profile = profileRepository.getProfile() ?: return@launch
                val userIdHash = profile.id.toMD5Bytes().copyOf(USER_ID_HASH_LENGTH)

                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // 低延迟模式
                    .setConnectable(true) // 允许其他设备发起 GATT 连接
                    .setTimeout(0) // 持续广播不超时
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM) // 中等发射功率
                    .build()
                val advertiseData = AdvertiseData.Builder()
                    .setIncludeDeviceName(false) // 广播包不包含设备名
                    .addServiceUuid(ParcelUuid(SERVICE_UUID)) // 声明服务 UUID，扫描方可按此过滤
                    .addServiceData(ParcelUuid(SERVICE_UUID), userIdHash) // 携带用户 ID 哈希
                    .build()

                // 开始广播
                bluetoothLeAdvertiser?.startAdvertising(
                    settings,
                    advertiseData,
                    object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                            Log.d(TAG, "BLE 广播已启动")
                        }

                        override fun onStartFailure(errorCode: Int) {
                            Log.e(TAG, "BLE 广播启动失败，错误码: $errorCode")
                        }
                    }
                )
            }.onFailure {
                Log.e(TAG, "启动 BLE 广播异常", it)
            }
        }
    }

    private fun checkBleReady(): Result<Unit> = runCatching {
        val adapter = bluetoothAdapter ?: throw IllegalStateException("蓝牙适配器不可用")

        if (!adapter.isEnabled) throw IllegalStateException("蓝牙未启用")

        bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser
            ?: throw IllegalStateException("设备不支持 BLE 广播")
    }.onFailure {
        it.message?.let { msg -> Log.w(TAG, msg) }
    }

    /**
     * 启动 GATT 服务
     *
     * GATT = Generic Attribute Profile（通用属性协议）
     * 是 BLE 设备之间交换数据的规范，定义了数据怎么组织、怎么读写。
     *
     * Characteristic（特征值）→ 具体数据字段，如"昵称""头像"
     * Descriptor（描述符） → 字段的元信息，如"是否开启通知"
     */
    private fun startGattServer() {
        try {
            val characteristic = BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or // 对方可以主动来读取数据
                        BluetoothGattCharacteristic.PROPERTY_WRITE or // 对方可以向这里写入数据
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY, // 数据变化时主动推送给对方，无需对方轮询
                BluetoothGattCharacteristic.PERMISSION_READ or // 读的权限
                        BluetoothGattCharacteristic.PERMISSION_WRITE // 写的权限
            ).apply {
                addDescriptor(
                    BluetoothGattDescriptor(
                        DESCRIPTOR_UUID,
                        BluetoothGattDescriptor.PERMISSION_READ or
                                BluetoothGattDescriptor.PERMISSION_WRITE
                    )
                )
            }

            // 创建服务
            val service = BluetoothGattService(
                SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY // 表示这是主服务，对外直接暴露
            ).apply {
                addCharacteristic(characteristic)
            }

            // 启动服务，开始监听连接
            gattServer = bluetoothManager.openGattServer(
                context,
                GattServerCallbackImpl() // 处理连接事件
            ).apply {
                addService(service)
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动 GATT 服务器异常", e)
        }
    }

    private inner class GattServerCallbackImpl : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                // 连接了
                BluetoothProfile.STATE_CONNECTED -> {
                    deviceSessions[device.address] = DeviceSession()
                }

                // 断开了
                BluetoothProfile.STATE_DISCONNECTED -> {
                    deviceSessions.remove(device.address)
                }
            }
        }

        /**
         * 对方设备向 Descriptor 写入数据时触发
         */
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            // 过滤无关写入
            if (descriptor.uuid == DESCRIPTOR_UUID) {
                // 有些客户端写入后需要一个确认回包，否则会超时报错
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,    // 回应的设备
                        requestId, // 对应哪次请求
                        BluetoothGatt.GATT_SUCCESS, // 告诉对方写入成功
                        0,
                        null // 不需要返回数据
                    )
                }

                when {
                    // 开启通知
                    value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) -> {
                        scope.launch {
                            sendProfileData(device)
                        }
                    }

                    // 关闭通知
                    value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) -> {
                        Log.d(TAG, "对方关闭了通知：${device.address}")
                    }
                }
            }
        }

        /**
         * 对方向 Characteristic 写入数据时触发
         */
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
                    // 没有会话时拒绝写入
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
                        // 接收JSON（元数据等）
                        handleJsonDataChunk(session, value)
                    } else {
                        // 接收二进制数据（头像等）
                        handleBinaryDataChunk(session, value)
                    }

                    // 回应成功，让对方继续发下一块
                    sendGattResponse(device, requestId, responseNeeded, BluetoothGatt.GATT_SUCCESS)
                } catch (e: Exception) {
                    Log.e(TAG, "处理写入请求失败: ${device.address}", e)
                    sendGattResponse(device, requestId, responseNeeded, BluetoothGatt.GATT_FAILURE)
                }
            }
        }

        @Suppress("BlockingMethodInNonBlockingContext")
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

        @Suppress("BlockingMethodInNonBlockingContext")
        private suspend fun handleBinaryDataChunk(session: DeviceSession, chunk: ByteArray) {
            session.buffer.write(chunk)
            if (session.buffer.size() >= session.expectedBinarySize) {
                val binaryData = session.buffer.toByteArray()
                handleCompleteMessage(session.currentMessage, binaryData)
                session.reset()
            }
        }

        private suspend fun handleCompleteMessage(
            message: FriendProtocol?,
            binaryData: ByteArray?
        ) {
            if (message == null) return
            try {
                when (message) {
                    is FriendProtocol.FriendRequest -> handleFriendRequest(message, binaryData)
                    is FriendProtocol.FriendResponse -> handleFriendResponse(message)
                    is FriendProtocol.ProfileRequest -> handleProfileRequest()
                    is FriendProtocol.ProfileResponse -> handleProfileResponse(message, binaryData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理消息失败: ${message::class.simpleName}", e)
            }
        }

        private suspend fun handleFriendRequest(
            message: FriendProtocol.FriendRequest,
            binaryData: ByteArray?
        ) {
//            friendRequestRepository.handleIncomingRequest(message.toDomain(binaryData))
            _friendEvents.emit(
                FriendEvent.FriendRequest(
                    nickname = message.nickname,
                    message = message.greeting
                )
            )
        }

        private suspend fun handleFriendResponse(message: FriendProtocol.FriendResponse) {
            friendRequestRepository.handleRequestResponse(message.toDomain())
            _friendEvents.emit(
                FriendEvent.FriendResponse(message.result)
            )
        }

        private suspend fun handleProfileRequest() {

        }

        private suspend fun handleProfileResponse(
            message: FriendProtocol.ProfileResponse,
            binaryData: ByteArray?
        ) {
//            friendRequestRepository.handleFullProfileResponse(message.toDomain(binaryData))
            Log.d(TAG, "收到完整资料响应")
        }

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

        private fun tryParseJsonMessage(jsonString: String): FriendProtocol? {
            return try {
                json.decodeFromString<FriendProtocol>(jsonString)
            } catch (e: Exception) {
                Log.w(TAG, "JSON 解析失败: ${e.message}")
                null
            }
        }

        private fun extractBinarySizeFromMessage(message: FriendProtocol): Int {
            return when (message) {
                is FriendProtocol.FriendRequest -> message.avatarSize
                is FriendProtocol.ProfileResponse -> message.avatarSize
                else -> 0
            }
        }

        // ==================== 主动发送资料 ====================

        /**
         * 客户端订阅 Notification 后，主动推送本方资料给对方（BLE雷达场景）
         */
        private suspend fun sendProfileData(device: BluetoothDevice) {
            try {
                val myProfile = profileRepository.getProfile() ?: return
                val avatarBytes = generateAvatarThumbnail(myProfile.avatarPath)

                val profileTransfer = UserProfileBeacon(
                    userId = myProfile.id,
                    nickname = myProfile.nickname,
                    signature = myProfile.signature,
                    gender = myProfile.gender,
                    avatarSize = avatarBytes?.size ?: 0,
                    publicKey = myProfile.publicKey
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
}

private class DeviceSession {
    var isReceivingBinary: Boolean = false
        private set

    val buffer: ByteArrayOutputStream = ByteArrayOutputStream()

    var currentMessage: FriendProtocol? = null
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

    fun setCurrentMessage(message: FriendProtocol) {
        currentMessage = message
    }
}