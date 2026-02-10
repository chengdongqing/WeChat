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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.scale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.Gender.Companion.getIndex
import top.chengdongqing.wechat.data.model.UserProfileTransfer
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.RequestAction
import top.chengdongqing.wechat.features.contacts.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.repository.ProfileRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class P2PService : Service() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var friendRequestRepository: FriendRequestRepository

    @Inject
    lateinit var json: Json

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    companion object {
        private const val TAG = "P2PService"

        // BLE 服务 UUID
        val SERVICE_UUID: UUID = UUID.fromString("0000FE9F-0000-1000-8000-00805F9B34FB")

        // 特征 UUID（用于读写数据）
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000FEA0-0000-1000-8000-00805F9B34FB")

        // Descriptor UUID（用于启用 Notification）
        val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val MAX_CHUNK_SIZE = 500  // 每次发送500字节

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "p2p_service_channel"

        // 好友请求通知
        private const val FRIEND_REQUEST_CHANNEL_ID = "friend_request_channel"
        private const val FRIEND_REQUEST_NOTIFICATION_ID = 2001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForegroundService()
        startBLEAdvertising()
        startGattServer()
    }

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
            .setContentTitle("局域网微信")
            .setContentText("P2P服务运行中")
            .setSmallIcon(R.drawable.img_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * 启动 BLE 广播
     */
    @SuppressLint("MissingPermission")
    private fun startBLEAdvertising() {
        serviceScope.launch {
            try {
                if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                    Log.w(TAG, "蓝牙不可用")
                    return@launch
                }

                bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
                if (bluetoothLeAdvertiser == null) {
                    Log.w(TAG, "设备不支持BLE广播")
                    return@launch
                }

                // 获取我的userId
                val myProfile = profileRepository.getCurrentProfile().first()
                if (myProfile == null) {
                    Log.w(TAG, "未找到个人资料")
                    return@launch
                }

                // 将userId哈希为16字节
                val userIdHash = myProfile.id.toMD5Bytes()

                // 只取前4字节作为广播数据（节省空间）
                val shortHash = userIdHash.copyOf(4)

                // 广播设置
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .build()

                // 广播数据
                val data = AdvertiseData.Builder()
                    .setIncludeDeviceName(false)
                    .addServiceUuid(ParcelUuid(SERVICE_UUID))
                    .addServiceData(ParcelUuid(SERVICE_UUID), shortHash)
                    .build()

                bluetoothLeAdvertiser!!.startAdvertising(
                    settings,
                    data,
                    object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                            Log.d(TAG, "BLE广播启动成功")
                        }

                        override fun onStartFailure(errorCode: Int) {
                            Log.e(TAG, "BLE广播启动失败: $errorCode")
                        }
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "启动BLE广播失败", e)
            }
        }
    }

    /**
     * 启动 GATT 服务器
     */
    @SuppressLint("MissingPermission")
    private fun startGattServer() {
        serviceScope.launch {
            try {
                val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

                val service = BluetoothGattService(
                    SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY
                )

                // 创建特征（支持 Read 和 Notify）
                val characteristic = BluetoothGattCharacteristic(
                    CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ or
                            BluetoothGattCharacteristic.PROPERTY_WRITE or
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ or
                            BluetoothGattCharacteristic.PERMISSION_WRITE
                )

                // 添加 Descriptor（用于启用 Notification）
                val descriptor = BluetoothGattDescriptor(
                    DESCRIPTOR_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or
                            BluetoothGattDescriptor.PERMISSION_WRITE
                )
                characteristic.addDescriptor(descriptor)

                service.addCharacteristic(characteristic)

                gattServer = bluetoothManager.openGattServer(
                    this@P2PService,
                    GattServerCallback()
                )

                gattServer?.addService(service)
                Log.d(TAG, "GATT服务器已启动")

            } catch (e: Exception) {
                Log.e(TAG, "启动GATT服务器失败", e)
            }
        }
    }

    /**
     * GATT 服务器回调
     */
    private inner class GattServerCallback : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "设备已连接: ${device.address}")
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
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
                Log.d(TAG, "客户端订阅了 Notification")

                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )
                }

                // 开始发送资料数据
                serviceScope.launch {
                    sendProfileData(device)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            gattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                0,
                "USE_NOTIFICATION".toByteArray()
            )
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
                    Log.d(TAG, "收到写入请求，数据大小: ${value.size}")

                    // 反序列化消息
                    when (val message = deserializeMessage(value)) {
                        is P2PMessage.FriendRequest -> {
                            Log.d(TAG, "收到好友申请: ${message.fromNickname}")
                            friendRequestRepository.handleIncomingRequest(message)
                            showFriendRequestNotification(
                                message.fromNickname,
                                "请求添加你为好友"
                            )
                        }

                        is P2PMessage.FriendRequestResponse -> {
                            Log.d(TAG, "收到好友申请响应: ${message.action}")
                            friendRequestRepository.handleRequestResponse(message)
                            if (message.action == RequestAction.ACCEPT) {
                                showFriendRequestNotification(
                                    "好友申请",
                                    "对方已同意你的好友申请"
                                )
                            }
                        }

                        else -> {
                            Log.d(TAG, "收到其他类型消息: ${message::class.simpleName}")
                        }
                    }

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
                    Log.e(TAG, "处理消息失败", e)
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
    }

    /**
     * 反序列化消息
     */
    private fun deserializeMessage(data: ByteArray): P2PMessage {
        return try {
            val jsonString = String(data, Charsets.UTF_8)
            Log.d(TAG, "反序列化消息: $jsonString")

            // 使用 Kotlinx Serialization 的多态反序列化
            json.decodeFromString<P2PMessage>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "反序列化失败", e)
            throw Exception("无法解析消息: ${e.message}")
        }
    }

    /**
     * 显示好友请求通知
     */
    private fun showFriendRequestNotification(title: String, content: String) {
        try {
            // 创建点击 Intent（打开新的朋友页面）
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // 可以添加额外参数，指示打开新的朋友页面
                putExtra("open_new_friends", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                FRIEND_REQUEST_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, FRIEND_REQUEST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_add_friends_filled)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(FRIEND_REQUEST_NOTIFICATION_ID, notification)

            Log.d(TAG, "已显示通知: $title - $content")
        } catch (e: Exception) {
            Log.e(TAG, "显示通知失败", e)
        }
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

    /**
     * 通过 Notification 发送数据
     */
    @SuppressLint("MissingPermission")
    private suspend fun sendProfileData(device: BluetoothDevice) {
        try {
            val myProfile = profileRepository.getCurrentProfile().first()

            if (myProfile == null) {
                Log.e(TAG, "未找到个人资料")
                return
            }

            val thumbnailBytes = myProfile.avatarPath?.let { path ->
                generateThumbnailBytes(path)
            }

            val transfer = UserProfileTransfer(
                userId = myProfile.id,
                nickname = myProfile.nickname,
                signature = myProfile.signature,
                gender = myProfile.gender.getIndex(),
                avatarSize = thumbnailBytes?.size ?: 0
            )

            val profileJson = json.encodeToString(transfer)
            val jsonBytes = profileJson.toByteArray(Charsets.UTF_8)

            Log.d(TAG, "开始发送数据:")
            Log.d(TAG, "  - JSON大小: ${jsonBytes.size} 字节")
            Log.d(TAG, "  - 头像大小: ${thumbnailBytes?.size ?: 0} 字节")
            Log.d(TAG, "  - 总大小: ${jsonBytes.size + (thumbnailBytes?.size ?: 0)} 字节")

            val service = gattServer?.getService(SERVICE_UUID)
            val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)

            if (characteristic == null || gattServer == null) {
                Log.e(TAG, "未找到特征或服务器")
                return
            }

            // 第一步：发送 JSON 元数据
            sendData(gattServer!!, device, characteristic, jsonBytes, "JSON")

            // 第二步：如果有头像，发送头像二进制
            if (thumbnailBytes != null) {
                delay(100)  // 短暂延迟
                sendData(gattServer!!, device, characteristic, thumbnailBytes, "头像")
            }

            Log.d(TAG, "✅ 全部数据发送完成")
        } catch (e: Exception) {
            Log.e(TAG, "发送数据失败", e)
        }
    }

    /**
     * 发送数据（分片）
     */
    private suspend fun sendData(
        server: BluetoothGattServer,
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        dataType: String
    ) {
        var offset = 0
        var chunkIndex = 0

        while (offset < data.size) {
            val remaining = data.size - offset
            val chunkSize = minOf(MAX_CHUNK_SIZE, remaining)
            val chunk = data.copyOfRange(offset, offset + chunkSize)

            val success = notifyCharacteristicChangedCompat(
                server,
                device,
                characteristic,
                value = chunk
            )

            if (success) {
                Log.d(
                    TAG,
                    "[$dataType] 片段 #$chunkIndex: $chunkSize 字节，剩余=${remaining - chunkSize}"
                )
            } else {
                Log.e(TAG, "[$dataType] 发送失败 #$chunkIndex")
                break
            }

            offset += chunkSize
            chunkIndex++

            delay(50)
        }
    }

    /**
     * 生成缩略图并转为Base64
     */
    private fun generateThumbnailBytes(
        imagePath: String,
        maxSizeKB: Int = 5,
        maxDimension: Int = 100
    ): ByteArray? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null

            // 读取图片
            val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null

            // 计算缩放比例
            val scale = minOf(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height
            )

            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()

            // 缩放
            val thumbnail = bitmap.scale(scaledWidth, scaledHeight)

            // 压缩为JPEG
            val outputStream = ByteArrayOutputStream()
            var quality = 80

            do {
                outputStream.reset()
                thumbnail.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                quality -= 10
            } while (outputStream.size() > maxSizeKB * 1024 && quality > 10)

            val bytes = outputStream.toByteArray()

            bitmap.recycle()
            thumbnail.recycle()

            bytes
        } catch (e: Exception) {
            Log.e(TAG, "生成缩略图失败", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        gattServer?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private fun String.toMD5Bytes(): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(this.toByteArray())
}