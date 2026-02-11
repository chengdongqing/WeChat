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
import top.chengdongqing.wechat.data.model.Gender.Companion.getIndex
import top.chengdongqing.wechat.data.model.UserProfileTransfer
import top.chengdongqing.wechat.data.network.protocol.P2PMessage
import top.chengdongqing.wechat.data.network.protocol.RequestAction
import top.chengdongqing.wechat.features.contacts.data.mapper.toDomain
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE 模块 - 负责好友添加功能
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
        const val TAG = "BLEModule"

        // UUID
        val SERVICE_UUID: UUID = UUID.fromString("0000FE9F-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000FEA0-0000-1000-8000-00805F9B34FB")
        val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // 配置
        const val MAX_CHUNK_SIZE = 500
        const val CHUNK_DELAY_MS = 50L
    }

    // 好友请求事件流
    private val _friendRequestEvents = MutableSharedFlow<FriendRequestEvent>()
    val friendRequestEvents: SharedFlow<FriendRequestEvent> = _friendRequestEvents.asSharedFlow()

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private val deviceSessions = mutableMapOf<String, DeviceSession>()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    /**
     * 启动 BLE 服务
     */
    suspend fun start(scope: CoroutineScope) {
        startBLEAdvertising(scope)
        startGattServer(scope)
        Log.d(TAG, "✅ BLE 模块已启动")
    }

    /**
     * 停止 BLE 服务
     */
    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
            gattServer?.close()
            deviceSessions.clear()
            Log.d(TAG, "BLE 模块已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止 BLE 失败", e)
        }
    }

    /**
     * 启动 BLE 广播
     */
    @SuppressLint("MissingPermission")
    private suspend fun startBLEAdvertising(scope: CoroutineScope) {
        scope.launch {
            try {
                val adapter = bluetoothAdapter ?: return@launch
                if (!adapter.isEnabled) {
                    Log.w(TAG, "蓝牙未启用")
                    return@launch
                }

                bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser
                if (bluetoothLeAdvertiser == null) {
                    Log.w(TAG, "设备不支持 BLE 广播")
                    return@launch
                }

                val myProfile = profileRepository.getCurrentProfile().first() ?: return@launch
                val userIdHash = myProfile.id.toMD5Bytes().copyOf(4)

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
                    settings,
                    data,
                    object : AdvertiseCallback() {
                        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                            Log.d(TAG, "✅ BLE 广播已启动")
                        }

                        override fun onStartFailure(errorCode: Int) {
                            Log.e(TAG, "BLE 广播启动失败: $errorCode")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "启动 BLE 广播失败", e)
            }
        }
    }

    /**
     * 启动 GATT 服务器
     */
    @SuppressLint("MissingPermission")
    private fun startGattServer(scope: CoroutineScope) {
        try {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

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

            gattServer = bluetoothManager.openGattServer(
                context,
                GattServerCallbackImpl(scope)
            )

            gattServer?.addService(service)
            Log.d(TAG, "✅ GATT 服务器已启动")

        } catch (e: Exception) {
            Log.e(TAG, "启动 GATT 服务器失败", e)
        }
    }

    /**
     * GATT 服务器回调
     */
    private inner class GattServerCallbackImpl(
        private val scope: CoroutineScope
    ) : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(
            device: android.bluetooth.BluetoothDevice,
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
            device: android.bluetooth.BluetoothDevice,
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

                // 发送个人资料
                scope.launch {
                    sendProfileData(device)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            scope.launch {
                try {
                    val session = deviceSessions[device.address] ?: return@launch

                    if (!session.isReceivingBinary) {
                        // 接收 JSON
                        session.buffer.write(value)
                        val jsonString = String(session.buffer.toByteArray(), Charsets.UTF_8)

                        if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                            try {
                                val message = json.decodeFromString<P2PMessage>(jsonString)
                                session.currentMessage = message

                                val binarySize = when (message) {
                                    is P2PMessage.FriendRequest -> message.avatarSize
                                    is P2PMessage.AutoAddResponse -> message.avatarSize
                                    is P2PMessage.FullProfileResponse -> message.avatarSize
                                    else -> 0
                                }

                                session.buffer.reset()

                                if (binarySize > 0) {
                                    session.isReceivingBinary = true
                                    session.expectedBinarySize = binarySize
                                } else {
                                    handleCompleteMessage(message, null)
                                    session.currentMessage = null
                                }
                            } catch (_: Exception) {
                                // JSON 未完成
                            }
                        }
                    } else {
                        // 接收二进制
                        session.buffer.write(value)

                        if (session.buffer.size() >= session.expectedBinarySize) {
                            val binaryData = session.buffer.toByteArray()
                            handleCompleteMessage(session.currentMessage, binaryData)

                            session.buffer.reset()
                            session.currentMessage = null
                            session.expectedBinarySize = 0
                            session.isReceivingBinary = false
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
                    Log.e(TAG, "处理写入失败", e)
                }
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

                    _friendRequestEvents.emit(
                        FriendRequestEvent.NewRequest(
                            nickname = message.peerNickname,
                            message = message.greetingMessage
                        )
                    )
                }

                is P2PMessage.FriendRequestResponse -> {
                    friendRequestRepository.handleRequestResponse(
                        message.toDomain()
                    )

                    if (message.action == RequestAction.ACCEPT) {
                        _friendRequestEvents.emit(
                            FriendRequestEvent.RequestAccepted("对方已同意你的好友申请")
                        )
                    }
                }

                is P2PMessage.AutoAddResponse -> {
                    friendRequestRepository.handleAutoAddResponse(
                        message.toDomain(binaryData)
                    )

                    _friendRequestEvents.emit(
                        FriendRequestEvent.AutoAdded(message.nickname)
                    )
                }

                is P2PMessage.FullProfileResponse -> {
                    friendRequestRepository.handleFullProfileResponse(
                        message.toDomain(binaryData)
                    )
                }

                else -> {}
            }
        }

        /**
         * 发送个人资料
         */
        @SuppressLint("MissingPermission")
        private suspend fun sendProfileData(device: android.bluetooth.BluetoothDevice) {
            try {
                val myProfile = profileRepository.getCurrentProfile().first() ?: return

                val thumbnailBytes = myProfile.avatarPath?.let { path ->
                    imageExt.generateThumbnailBytes(path, targetSize = 100, maxSizeKB = 5)
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

                val service = gattServer?.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)

                if (characteristic != null && gattServer != null) {
                    // 发送 JSON
                    sendDataChunked(gattServer!!, device, characteristic, jsonBytes, "JSON")

                    // 发送头像
                    if (thumbnailBytes != null) {
                        delay(100)
                        sendDataChunked(
                            gattServer!!,
                            device,
                            characteristic,
                            thumbnailBytes,
                            "Avatar"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送资料失败", e)
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
    }

    /**
     * 设备会话
     */
    private data class DeviceSession(
        var isReceivingBinary: Boolean = false,
        val buffer: ByteArrayOutputStream = ByteArrayOutputStream(),
        var currentMessage: P2PMessage? = null,
        var expectedBinarySize: Int = 0
    )
}

/**
 * 好友请求事件
 */
sealed class FriendRequestEvent {
    data class NewRequest(val nickname: String, val message: String) : FriendRequestEvent()
    data class RequestAccepted(val message: String) : FriendRequestEvent()
    data class AutoAdded(val nickname: String) : FriendRequestEvent()
}