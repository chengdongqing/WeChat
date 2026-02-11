package top.chengdongqing.wechat.data.network.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.ImageExt
import top.chengdongqing.wechat.data.model.Gender.Companion.getIndex
import top.chengdongqing.wechat.data.model.UserProfileTransfer
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.RequestAction
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

/**
 * P2P 通信服务
 *
 * 职责：
 * - 通过 BLE 进行设备发现和连接
 * - 处理 P2P 消息的接收和发送
 * - 管理 GATT 服务器和客户端连接
 */
@AndroidEntryPoint
class P2PService : Service() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var friendRequestRepository: FriendRequestRepository

    @Inject
    lateinit var bleDiscovery: BLEDiscovery

    @Inject
    lateinit var imageExt: ImageExt

    @Inject
    lateinit var json: Json

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    companion object {
        const val TAG = "P2PService"

        // UUID
        val SERVICE_UUID: UUID = UUID.fromString("0000FE9F-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000FEA0-0000-1000-8000-00805F9B34FB")
        val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // 配置
        const val MAX_CHUNK_SIZE = 500
        const val CHUNK_DELAY_MS = 50L

        // 通知
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "p2p_service_channel"
        const val FRIEND_REQUEST_CHANNEL_ID = "friend_request_channel"
        const val FRIEND_REQUEST_NOTIFICATION_ID = 2001
    }

    // ==================== 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForegroundService()
        startBLEAdvertising()
        startGattServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBLEAdvertising()
        stopGattServer()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // P2P 服务通道
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "P2P连接服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持局域网连接功能运行"
                setShowBadge(false)
            }

            // 好友请求通道
            val friendRequestChannel = NotificationChannel(
                FRIEND_REQUEST_CHANNEL_ID,
                "好友请求",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "新的好友请求通知"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(friendRequestChannel)
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "P2P连接服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持局域网连接功能运行"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("P2P服务运行中")
            .setSmallIcon(R.drawable.img_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    // ==================== BLE 广播 ====================

    @SuppressLint("MissingPermission")
    private fun startBLEAdvertising() {
        serviceScope.launch {
            try {
                val adapter = bluetoothAdapter ?: return@launch logError("蓝牙不可用")
                if (!adapter.isEnabled) return@launch logWarn("蓝牙未启用")

                val advertiser = adapter.bluetoothLeAdvertiser
                    ?: return@launch logWarn("设备不支持BLE广播")

                // 获取用户ID哈希
                val myProfile = profileRepository.getCurrentProfile().first()
                    ?: return@launch logWarn("未找到个人资料")

                val userIdHash = myProfile.id.toMD5Bytes().copyOf(4)

                // 配置广播
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

                advertiser.startAdvertising(settings, data, AdvertiseCallbackImpl())
                bluetoothLeAdvertiser = advertiser

                Log.d(TAG, "BLE广播已启动")
            } catch (e: Exception) {
                logError("启动BLE广播失败", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBLEAdvertising() {
        bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        bluetoothLeAdvertiser = null
    }

    // ==================== GATT 服务器 ====================

    @SuppressLint("MissingPermission")
    private fun startGattServer() {
        serviceScope.launch {
            try {
                val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

                // 创建服务
                val service = createGattService()

                // 打开服务器
                gattServer = manager.openGattServer(this@P2PService, GattServerCallback())
                gattServer?.addService(service)

                Log.d(TAG, "GATT服务器已启动")
            } catch (e: Exception) {
                logError("启动GATT服务器失败", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopGattServer() {
        gattServer?.close()
        gattServer = null
    }

    /**
     * 创建 GATT 服务
     */
    private fun createGattService(): BluetoothGattService {
        val service = BluetoothGattService(
            SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val characteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val descriptor = BluetoothGattDescriptor(
            DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or
                    BluetoothGattDescriptor.PERMISSION_WRITE
        )

        characteristic.addDescriptor(descriptor)
        service.addCharacteristic(characteristic)

        return service
    }

    // ==================== GATT 回调 ====================

    private inner class GattServerCallback : BluetoothGattServerCallback() {

        private val deviceSessions = mutableMapOf<String, DeviceSession>()

        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    deviceSessions[device.address] = DeviceSession()
                    Log.d(TAG, "设备已连接: ${device.address}")
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
                Log.d(TAG, "客户端订阅了通知")

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }

                // 发送资料
                serviceScope.launch { sendProfileData(device) }
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
            serviceScope.launch {
                try {
                    val session = deviceSessions[device.address]
                        ?: return@launch logError("未找到设备会话")

                    handleDataReceive(session, value)

                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null
                        )
                    }
                } catch (e: Exception) {
                    logError("处理写入失败", e)
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null
                        )
                    }
                }
            }
        }

        /**
         * 处理接收数据
         */
        private suspend fun handleDataReceive(session: DeviceSession, value: ByteArray) {
            if (!session.isReceivingBinary) {
                handleJsonReceive(session, value)
            } else {
                handleBinaryReceive(session, value)
            }
        }

        /**
         * 处理 JSON 接收
         */
        private suspend fun handleJsonReceive(session: DeviceSession, value: ByteArray) {
            session.buffer.write(value)

            val jsonString = String(session.buffer.toByteArray(), Charsets.UTF_8)

            if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                try {
                    val message = json.decodeFromString<P2PMessage>(jsonString)
                    session.currentMessage = message

                    Log.d(TAG, "✅ 收到消息: ${message::class.simpleName}")

                    // 检查是否有二进制数据
                    val binarySize = message.getBinarySize()

                    session.buffer.reset()

                    if (binarySize > 0) {
                        session.isReceivingBinary = true
                        session.expectedBinarySize = binarySize
                        Log.d(TAG, "等待二进制: $binarySize 字节")
                    } else {
                        handleCompleteMessage(message, null)
                        session.currentMessage = null
                    }
                } catch (_: Exception) {
                    Log.d(TAG, "JSON 未完成，继续接收")
                }
            }
        }

        /**
         * 处理二进制接收
         */
        private suspend fun handleBinaryReceive(session: DeviceSession, value: ByteArray) {
            session.buffer.write(value)

            Log.d(TAG, "接收二进制: ${session.buffer.size()}/${session.expectedBinarySize}")

            if (session.buffer.size() >= session.expectedBinarySize) {
                val binaryData = session.buffer.toByteArray()
                Log.d(TAG, "✅ 二进制接收完成: ${binaryData.size} 字节")

                handleCompleteMessage(session.currentMessage, binaryData)

                // 重置会话
                session.reset()
            }
        }

        /**
         * 处理完整消息
         */
        private suspend fun handleCompleteMessage(message: P2PMessage?, binaryData: ByteArray?) {
            when (message) {
                is P2PMessage.FriendRequest -> {
                    friendRequestRepository.handleIncomingRequest(
                        message.toDomain(binaryData)
                    )
                    showNotification("${message.peerNickname}请求添加你为好友")
                }

                is P2PMessage.FriendRequestResponse -> {
                    friendRequestRepository.handleRequestResponse(
                        message.toDomain()
                    )
                    if (message.action == RequestAction.ACCEPT) {
                        showNotification("对方已同意你的好友申请")
                    }
                }

                is P2PMessage.AutoAddResponse -> {
                    friendRequestRepository.handleAutoAddResponse(
                        message.toDomain(binaryData)
                    )
                }

                is P2PMessage.FullProfileResponse -> {
                    friendRequestRepository.handleFullProfileResponse(
                        message.toDomain(binaryData)
                    )
                }

                else -> logWarn("未知消息类型")
            }
        }
    }

    // ==================== 发送数据 ====================

    @SuppressLint("MissingPermission")
    private suspend fun sendProfileData(device: BluetoothDevice) {
        try {
            val myProfile = profileRepository.getCurrentProfile().first()
                ?: return logError("未找到个人资料")

            val thumbnailBytes = myProfile.avatarPath?.let {
                imageExt.generateThumbnailBytes(it, targetSize = 100, maxSizeKB = 5)
            }

            val transfer = UserProfileTransfer(
                userId = myProfile.id,
                nickname = myProfile.nickname,
                signature = myProfile.signature,
                gender = myProfile.gender.getIndex(),
                avatarSize = thumbnailBytes?.size ?: 0
            )

            val jsonBytes = json.encodeToString(transfer).toByteArray()

            Log.d(TAG, "发送数据: JSON=${jsonBytes.size}B, 头像=${thumbnailBytes?.size ?: 0}B")

            val service = gattServer?.getService(SERVICE_UUID)
            val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)

            if (characteristic == null || gattServer == null) {
                return logError("未找到特征或服务器")
            }

            // 发送 JSON
            sendDataChunked(gattServer!!, device, characteristic, jsonBytes, "JSON")

            // 发送头像
            if (thumbnailBytes != null) {
                delay(100)
                sendDataChunked(gattServer!!, device, characteristic, thumbnailBytes, "头像")
            }

            Log.d(TAG, "✅ 数据发送完成")
        } catch (e: Exception) {
            logError("发送数据失败", e)
        }
    }

    /**
     * 分片发送数据
     */
    private suspend fun sendDataChunked(
        server: BluetoothGattServer,
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        dataType: String
    ) {
        var offset = 0
        var chunkIndex = 0

        while (offset < data.size) {
            val chunkSize = minOf(MAX_CHUNK_SIZE, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkSize)

            val success =
                notifyCharacteristicChangedCompat(server, device, characteristic, value = chunk)

            if (!success) {
                logError("[$dataType] 发送失败 #$chunkIndex")
                break
            }

            offset += chunkSize
            chunkIndex++
            delay(CHUNK_DELAY_MS)
        }

        Log.d(TAG, "[$dataType] 发送完成: $chunkIndex 片")
    }

    /**
     * 兼容新旧 API 的 Notification 发送
     */
    @SuppressLint("MissingPermission")
    private fun notifyCharacteristicChangedCompat(
        server: BluetoothGattServer,
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        confirm: Boolean = false,
        value: ByteArray
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            server.notifyCharacteristicChanged(
                device,
                characteristic,
                confirm,
                value
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            (server.notifyCharacteristicChanged(device, characteristic, confirm))
        }
    }

    // ==================== 通知 ====================

    private fun showNotification(content: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "wechat://contacts/new".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                FRIEND_REQUEST_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, FRIEND_REQUEST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_add_friends_filled)
                .setContentTitle("好友请求")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(FRIEND_REQUEST_NOTIFICATION_ID, notification)

            Log.d(TAG, "已显示通知: $content")
        } catch (e: Exception) {
            logError("显示通知失败", e)
        }
    }

    // ==================== 辅助方法 ====================

    private fun logError(message: String, e: Exception? = null) {
        if (e != null) Log.e(TAG, message, e) else Log.e(TAG, message)
    }

    private fun logWarn(message: String) {
        Log.w(TAG, message)
    }

    /**
     * 获取消息的二进制大小
     */
    private fun P2PMessage.getBinarySize(): Int = when (this) {
        is P2PMessage.FriendRequest -> avatarSize
        is P2PMessage.FullProfileResponse -> avatarSize
        is P2PMessage.AutoAddResponse -> avatarSize
        else -> 0
    }

    // ==================== 内部类 ====================

    /**
     * 设备会话
     */
    private data class DeviceSession(
        var isReceivingBinary: Boolean = false,
        val buffer: ByteArrayOutputStream = ByteArrayOutputStream(),
        var currentMessage: P2PMessage? = null,
        var expectedBinarySize: Int = 0
    ) {
        fun reset() {
            buffer.reset()
            currentMessage = null
            expectedBinarySize = 0
            isReceivingBinary = false
        }
    }

    /**
     * 广播回调
     */
    private class AdvertiseCallbackImpl : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "BLE广播启动成功")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE广播启动失败: $errorCode")
        }
    }
}

// ==================== 扩展函数 ====================

private fun String.toMD5Bytes(): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(this.toByteArray())
}