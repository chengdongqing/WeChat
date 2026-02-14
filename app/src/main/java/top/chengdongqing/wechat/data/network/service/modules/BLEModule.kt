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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
 * 1. 启动 BLE 广播,向附近设备广播用户身份(MD5 哈希)
 * 2. 启动 GATT 服务器,接收和发送好友请求数据
 * 3. 管理设备会话,处理数据分片传输
 * 4. 支持两阶段数据传输: JSON 元数据 + 二进制数据(如头像)
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

        // UUID 定义
        val SERVICE_UUID: UUID = UUID.fromString("0000FE9F-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000FEA0-0000-1000-8000-00805F9B34FB")
        val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // 数据传输配置
        private const val MAX_CHUNK_SIZE = 500
        private const val CHUNK_DELAY_MS = 50L
        private const val PROFILE_SEND_DELAY_MS = 100L

        // 用户 ID 哈希配置
        private const val USER_ID_HASH_LENGTH = 4

        // 头像缩略图配置
        private const val AVATAR_THUMBNAIL_SIZE = 100
        private const val AVATAR_MAX_SIZE_KB = 5

        // JSON 识别标记
        private const val JSON_START_MARKER = '{'
        private const val JSON_END_MARKER = '}'
    }

    // 好友请求事件流
    private val _friendRequestEvents = MutableSharedFlow<FriendRequestEvent>()
    val friendRequestEvents: SharedFlow<FriendRequestEvent> = _friendRequestEvents.asSharedFlow()

    // BLE 组件
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null

    // 设备会话管理
    private val deviceSessions = ConcurrentHashMap<String, DeviceSession>()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    /**
     * 启动 BLE 服务
     *
     * 启动 BLE 广播和 GATT 服务器
     * 广播用于让附近设备发现本设备
     * GATT 服务器用于数据交换
     */
    fun start(scope: CoroutineScope) {
        startBLEAdvertising(scope)
        startGattServer(scope)
        Log.d(TAG, "✅ BLE 模块已启动")
    }

    /**
     * 停止 BLE 服务
     *
     * 停止广播、关闭 GATT 服务器、清理所有设备会话
     */
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

    /**
     * 启动 BLE 广播
     *
     * 广播内容包括:
     * - Service UUID: 标识 WeChat 服务
     * - Service Data: 用户 ID 的 MD5 哈希前 4 字节(用于去重检测)
     *
     * 广播配置:
     * - 低延迟模式: 快速响应连接
     * - 高功率: 增加扫描范围
     * - 可连接: 允许其他设备连接
     */
    @SuppressLint("MissingPermission")
    private fun startBLEAdvertising(scope: CoroutineScope) {
        scope.launch {
            try {
                // 检查蓝牙适配器
                val adapter = bluetoothAdapter
                if (adapter == null) {
                    Log.w(TAG, "蓝牙适配器不可用")
                    return@launch
                }
                if (!adapter.isEnabled) {
                    Log.w(TAG, "蓝牙未启用")
                    return@launch
                }

                // 检查 BLE 广播支持
                bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser
                if (bluetoothLeAdvertiser == null) {
                    Log.w(TAG, "设备不支持 BLE 广播")
                    return@launch
                }

                // 获取用户 ID 哈希
                val myProfile = profileRepository.getCurrentProfile().first()
                if (myProfile == null) {
                    Log.w(TAG, "无法获取个人资料,无法启动广播")
                    return@launch
                }
                val userIdHash = myProfile.id.toMD5Bytes().copyOf(USER_ID_HASH_LENGTH)

                // 配置广播设置
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(true)
                    .setTimeout(0) // 持续广播
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .build()

                // 配置广播数据
                val data = AdvertiseData.Builder()
                    .setIncludeDeviceName(false) // 不包含设备名,节省空间
                    .addServiceUuid(ParcelUuid(SERVICE_UUID))
                    .addServiceData(ParcelUuid(SERVICE_UUID), userIdHash)
                    .build()

                // 启动广播
                bluetoothLeAdvertiser!!.startAdvertising(
                    settings,
                    data,
                    object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                            Log.d(TAG, "✅ BLE 广播已启动")
                        }

                        override fun onStartFailure(errorCode: Int) {
                            Log.e(TAG, "BLE 广播启动失败,错误码: $errorCode")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "启动 BLE 广播异常", e)
            }
        }
    }

    /**
     * 启动 GATT 服务器
     *
     * GATT 服务器结构:
     * - Service: 主服务
     *   - Characteristic: 数据特征值
     *     - Descriptor: 通知描述符(用于订阅通知)
     *
     * 特征值属性:
     * - READ: 客户端可读取
     * - WRITE: 客户端可写入
     * - NOTIFY: 服务器可主动推送
     */
    @SuppressLint("MissingPermission")
    private fun startGattServer(scope: CoroutineScope) {
        try {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            // 创建服务
            val service = BluetoothGattService(
                SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            // 创建特征值
            val characteristic = BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            // 创建通知描述符
            val descriptor = BluetoothGattDescriptor(
                DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or
                        BluetoothGattDescriptor.PERMISSION_WRITE
            )

            // 组装 GATT 结构
            characteristic.addDescriptor(descriptor)
            service.addCharacteristic(characteristic)

            // 启动 GATT 服务器
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

    /**
     * GATT 服务器回调处理器
     *
     * 处理客户端的连接、断开、读写请求等事件
     */
    private inner class GattServerCallbackImpl(
        private val scope: CoroutineScope
    ) : BluetoothGattServerCallback() {

        /**
         * 连接状态变化回调
         *
         * 连接时创建设备会话,断开时清理会话
         */
        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int
        ) {
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

        /**
         * 描述符写入请求回调
         *
         * 客户端订阅通知时触发
         * 订阅成功后,自动发送个人资料给客户端
         */
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

                // 发送成功响应
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }

                // 自动发送个人资料
                scope.launch {
                    sendProfileData(device)
                }
            }
        }

        /**
         * 处理客户端写入请求
         *
         * 数据接收采用两阶段模式:
         * 1. 第一阶段: 接收 JSON 元数据,解析出消息类型和二进制数据大小
         * 2. 第二阶段: 如果有二进制数据(如头像),继续接收二进制数据
         *
         * 数据可能分片传输,需要在缓冲区中累积直到完整
         */
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

                    // 根据当前状态处理数据
                    if (!session.isReceivingBinary) {
                        handleJsonDataChunk(session, value)
                    } else {
                        handleBinaryDataChunk(session, value)
                    }

                    // 发送响应
                    sendGattResponse(device, requestId, responseNeeded, BluetoothGatt.GATT_SUCCESS)
                } catch (e: Exception) {
                    Log.e(TAG, "处理写入请求失败: ${device.address}", e)
                    sendGattResponse(device, requestId, responseNeeded, BluetoothGatt.GATT_FAILURE)
                }
            }
        }

        /**
         * 处理 JSON 数据分片
         *
         * 累积接收的数据直到形成完整的 JSON 对象
         * JSON 对象以 '{' 开始,以 '}' 结束
         */
        private suspend fun handleJsonDataChunk(session: DeviceSession, chunk: ByteArray) {
            session.buffer.write(chunk)
            val jsonString = String(session.buffer.toByteArray(), Charsets.UTF_8)

            // 检查是否收到完整 JSON
            if (isCompleteJson(jsonString)) {
                val message = tryParseJsonMessage(jsonString)
                if (message != null) {
                    session.setCurrentMessage(message)
                    session.buffer.reset()

                    // 检查是否需要接收二进制数据
                    val binarySize = extractBinarySizeFromMessage(message)
                    if (binarySize > 0) {
                        session.transitionToBinaryMode(binarySize)
                    } else {
                        handleCompleteMessage(message, null)
                        session.reset()
                    }
                }
            }
        }

        /**
         * 处理二进制数据分片
         *
         * 累积接收的数据直到达到预期大小
         */
        private suspend fun handleBinaryDataChunk(session: DeviceSession, chunk: ByteArray) {
            session.buffer.write(chunk)

            // 检查是否收到完整二进制数据
            if (session.buffer.size() >= session.expectedBinarySize) {
                val binaryData = session.buffer.toByteArray()
                handleCompleteMessage(session.currentMessage, binaryData)
                session.reset()
            }
        }

        /**
         * 发送 GATT 响应
         */
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

        /**
         * 检查是否为完整 JSON
         */
        private fun isCompleteJson(jsonString: String): Boolean {
            return jsonString.startsWith(JSON_START_MARKER) && jsonString.endsWith(JSON_END_MARKER)
        }

        /**
         * 尝试解析 JSON 消息
         */
        private fun tryParseJsonMessage(jsonString: String): P2PMessage? {
            return try {
                json.decodeFromString<P2PMessage>(jsonString)
            } catch (e: Exception) {
                Log.w(TAG, "JSON 解析失败: ${e.message}")
                null
            }
        }

        /**
         * 从消息中提取二进制数据大小
         */
        private fun extractBinarySizeFromMessage(message: P2PMessage): Int {
            return when (message) {
                is P2PMessage.FriendRequest -> message.avatarSize
                is P2PMessage.AutoAddResponse -> message.avatarSize
                is P2PMessage.FullProfileResponse -> message.avatarSize
                else -> 0
            }
        }

        /**
         * 处理完整消息(JSON + 可选的二进制数据)
         *
         * 根据消息类型分发到对应的处理器,并触发相应的事件
         */
        private suspend fun handleCompleteMessage(message: P2PMessage?, binaryData: ByteArray?) {
            if (message == null) {
                Log.w(TAG, "消息为空,无法处理")
                return
            }

            try {
                when (message) {
                    is P2PMessage.FriendRequest -> handleFriendRequest(message, binaryData)
                    is P2PMessage.FriendRequestResponse -> handleFriendRequestResponse(message)
                    is P2PMessage.AutoAddResponse -> handleAutoAddResponse(message, binaryData)
                    is P2PMessage.FullProfileResponse -> handleFullProfileResponse(
                        message,
                        binaryData
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理消息失败: ${message::class.simpleName}", e)
            }
        }

        /**
         * 处理好友请求
         */
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

        /**
         * 处理好友请求响应
         */
        private suspend fun handleFriendRequestResponse(message: P2PMessage.FriendRequestResponse) {
            friendRequestRepository.handleRequestResponse(message.toDomain())
            if (message.action == RequestAction.ACCEPT) {
                _friendRequestEvents.emit(
                    FriendRequestEvent.RequestAccepted("对方已同意你的好友申请")
                )
                Log.d(TAG, "好友请求已被接受")
            }
        }

        /**
         * 处理自动添加响应
         */
        private suspend fun handleAutoAddResponse(
            message: P2PMessage.AutoAddResponse,
            binaryData: ByteArray?
        ) {
            friendRequestRepository.handleAutoAddResponse(message.toDomain(binaryData))
            _friendRequestEvents.emit(FriendRequestEvent.AutoAdded(message.nickname))
            Log.d(TAG, "自动添加好友: ${message.nickname}")
        }

        /**
         * 处理完整资料响应
         */
        private suspend fun handleFullProfileResponse(
            message: P2PMessage.FullProfileResponse,
            binaryData: ByteArray?
        ) {
            friendRequestRepository.handleFullProfileResponse(message.toDomain(binaryData))
            Log.d(TAG, "收到完整资料响应")
        }

        /**
         * 发送个人资料给连接的设备
         *
         * 发送流程:
         * 1. 生成头像缩略图
         * 2. 构建资料传输对象(包含头像大小)
         * 3. 先发送 JSON 元数据
         * 4. 再发送头像二进制数据
         */
        @SuppressLint("MissingPermission")
        private suspend fun sendProfileData(device: BluetoothDevice) {
            try {
                val myProfile = profileRepository.getCurrentProfile().first()
                if (myProfile == null) {
                    Log.w(TAG, "无法获取个人资料")
                    return
                }

                // 生成头像缩略图
                val avatarBytes = generateAvatarThumbnail(myProfile.avatarPath)

                // 构建资料传输对象
                val profileTransfer = UserProfileTransfer(
                    userId = myProfile.id,
                    nickname = myProfile.nickname,
                    signature = myProfile.signature,
                    gender = myProfile.gender.getIndex(),
                    avatarSize = avatarBytes?.size ?: 0
                )

                // 获取特征值
                val characteristic = getCharacteristic()
                if (characteristic == null || gattServer == null) {
                    Log.w(TAG, "GATT 服务器或特征值不可用")
                    return
                }

                // 发送 JSON 元数据
                val jsonBytes = json.encodeToString(profileTransfer).toByteArray(Charsets.UTF_8)
                sendDataChunked(gattServer!!, device, characteristic, jsonBytes, "ProfileJSON")

                // 发送头像数据
                if (avatarBytes != null) {
                    delay(PROFILE_SEND_DELAY_MS)
                    sendDataChunked(gattServer!!, device, characteristic, avatarBytes, "Avatar")
                }

                Log.d(TAG, "资料发送完成: ${device.address}")
            } catch (e: Exception) {
                Log.e(TAG, "发送资料失败: ${device.address}", e)
            }
        }

        /**
         * 生成头像缩略图
         */
        private suspend fun generateAvatarThumbnail(avatarPath: String?): ByteArray? {
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

        /**
         * 获取特征值
         */
        private fun getCharacteristic(): BluetoothGattCharacteristic? {
            val service = gattServer?.getService(SERVICE_UUID)
            return service?.getCharacteristic(CHARACTERISTIC_UUID)
        }

        /**
         * 分片发送数据
         *
         * 将大数据拆分为小块,逐个发送给客户端
         * 每个分片之间有延迟,避免蓝牙缓冲区溢出
         *
         * @param server GATT 服务器
         * @param device 目标设备
         * @param characteristic 特征值
         * @param data 要发送的数据
         * @param dataType 数据类型描述(用于日志)
         */
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
                val remainingSize = data.size - offset
                val chunkSize = minOf(MAX_CHUNK_SIZE, remainingSize)
                val chunk = data.copyOfRange(offset, offset + chunkSize)

                // 发送分片
                val success = notifyCharacteristicChangedCompat(
                    server = server,
                    device = device,
                    characteristic = characteristic,
                    value = chunk
                )

                if (!success) {
                    Log.w(TAG, "[$dataType] 发送失败,已发送 $chunkCount 片")
                    break
                }

                offset += chunkSize
                chunkCount++

                // 延迟避免缓冲区溢出
                if (offset < data.size) {
                    delay(CHUNK_DELAY_MS)
                }
            }

            Log.d(TAG, "[$dataType] 发送完成: $chunkCount 片,总大小 ${data.size} 字节")
        }

        /**
         * 兼容新旧 API 的 Notification 发送
         *
         * Android 13 (API 33) 引入了新的通知 API
         * 此方法兼容新旧两种 API
         *
         * @param server GATT 服务器
         * @param device 目标设备
         * @param characteristic 特征值
         * @param confirm 是否需要确认(false = Notification, true = Indication)
         * @param value 要发送的数据
         * @return 发送是否成功
         */
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
                    // Android 13+ 使用新 API
                    server.notifyCharacteristicChanged(
                        device,
                        characteristic,
                        confirm,
                        value
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    // Android 12 及以下使用旧 API
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

    /**
     * 设备会话状态管理
     *
     * 管理与单个设备的数据接收状态
     * 包括: JSON 接收、二进制接收的状态切换和缓冲区管理
     */
    private class DeviceSession {
        // 接收状态
        var isReceivingBinary: Boolean = false
            private set

        // 数据缓冲区
        val buffer: ByteArrayOutputStream = ByteArrayOutputStream()

        // 当前正在处理的消息
        var currentMessage: P2PMessage? = null
            private set

        // 预期接收的二进制数据大小
        var expectedBinarySize: Int = 0
            private set

        /**
         * 切换到二进制接收模式
         */
        fun transitionToBinaryMode(binarySize: Int) {
            isReceivingBinary = true
            expectedBinarySize = binarySize
        }

        /**
         * 重置会话状态
         */
        fun reset() {
            buffer.reset()
            currentMessage = null
            expectedBinarySize = 0
            isReceivingBinary = false
        }

        /**
         * 设置当前消息
         */
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
     * 收到新的好友请求
     *
     * @param nickname 请求者昵称
     * @param message 验证消息
     */
    data class NewRequest(val nickname: String, val message: String) : FriendRequestEvent()

    /**
     * 好友请求被接受
     *
     * @param message 提示消息
     */
    data class RequestAccepted(val message: String) : FriendRequestEvent()

    /**
     * 好友自动添加成功
     *
     * @param nickname 好友昵称
     */
    data class AutoAdded(val nickname: String) : FriendRequestEvent()
}