package top.chengdongqing.wechat.data.network.discovery

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import top.chengdongqing.wechat.data.network.service.modules.BLEModule
import top.chengdongqing.wechat.features.me.data.model.UserProfileTransfer
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume

class BLEDiscovery @Inject constructor(
    private val json: Json,
    @param:ApplicationContext private val context: Context
) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var readContinuation: CancellableContinuation<Pair<UserProfileTransfer, ByteArray>?>? =
        null
    private var writeContinuation: CancellableContinuation<Boolean>? = null

    private val receivedData = ByteArrayOutputStream()

    // 解析状态
    private var jsonReceived = false
    private var profileTransfer: UserProfileTransfer? = null

    companion object {
        private const val TAG = "BLEDiscovery"
        private const val MAX_WRITE_SIZE = 512
    }

    /**
     * 扫描并连接到目标设备
     */
    @SuppressLint("MissingPermission")
    suspend fun scanAndConnect(targetUserIdHash: String): BluetoothGatt? {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->

                Log.d(TAG, "开始扫描，目标哈希: ${targetUserIdHash.take(8)}")

                if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                    Log.e(TAG, "蓝牙不可用")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner

                if (bluetoothLeScanner == null) {
                    Log.e(TAG, "BLE扫描器不可用")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                val filter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(BLEModule.SERVICE_UUID))
                    .build()

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    .setReportDelay(0)
                    .build()

                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        Log.d(TAG, "扫描到设备: ${result.device.address}")

                        val serviceData = result.scanRecord?.getServiceData(
                            ParcelUuid(BLEModule.SERVICE_UUID)
                        )

                        if (serviceData != null) {
                            val advertisedHash = serviceData.toHexString()
                            val targetHash = targetUserIdHash.take(8)

                            Log.d(TAG, "设备哈希: $advertisedHash, 目标哈希: $targetHash")

                            if (advertisedHash == targetHash) {
                                Log.d(TAG, "找到目标设备！")
                                bluetoothLeScanner?.stopScan(this)
                                connectToDevice(result.device, continuation)
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "扫描失败: $errorCode")
                        continuation.resume(null)
                    }
                }

                try {
                    Log.d(TAG, "启动BLE扫描...")
                    bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)

                    continuation.invokeOnCancellation {
                        bluetoothLeScanner?.stopScan(scanCallback)
                    }

                    // 10秒超时
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(10000)
                        if (continuation.isActive) {
                            Log.d(TAG, "扫描超时")
                            bluetoothLeScanner?.stopScan(scanCallback)
                            continuation.resume(null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "启动扫描失败", e)
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * 写入特征值
     */
    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ): Boolean {
        return try {
            if (data.size <= MAX_WRITE_SIZE) {
                // 小于最大值，直接写入
                Log.d(TAG, "单次写入: ${data.size} 字节")
                writeCharacteristicOnce(gatt, characteristic, data)
            } else {
                // 分片写入
                Log.d(TAG, "开始分片写入，总大小: ${data.size} 字节")

                var offset = 0
                var chunkIndex = 0

                while (offset < data.size) {
                    val remaining = data.size - offset
                    val chunkSize = minOf(MAX_WRITE_SIZE, remaining)
                    val chunk = data.copyOfRange(offset, offset + chunkSize)

                    Log.d(
                        TAG,
                        "写入片段 #$chunkIndex: $chunkSize 字节, 剩余: ${remaining - chunkSize}"
                    )

                    val success = writeCharacteristicOnce(gatt, characteristic, chunk)

                    if (!success) {
                        Log.e(TAG, "片段 #$chunkIndex 写入失败")
                        return false
                    }

                    offset += chunkSize
                    chunkIndex++

                    // 每片之间延迟 50ms
                    delay(50)
                }

                Log.d(TAG, "✅ 分片写入完成，共 $chunkIndex 片")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入失败", e)
            false
        }
    }

    /**
     * 写入特征值（单次）
     */
    @SuppressLint("MissingPermission")
    private suspend fun writeCharacteristicOnce(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->

            writeContinuation = continuation

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }

            if (!result) {
                Log.e(TAG, "启动写入失败")
                writeContinuation = null
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            // 10秒超时
            CoroutineScope(Dispatchers.IO).launch {
                delay(10000)
                if (writeContinuation == continuation && continuation.isActive) {
                    Log.e(TAG, "写入超时")
                    writeContinuation = null
                    continuation.resume(false)
                }
            }
        }
    }

    /**
     * 连接到设备
     */
    @SuppressLint("MissingPermission")
    private fun connectToDevice(
        device: BluetoothDevice,
        continuation: CancellableContinuation<BluetoothGatt?>
    ) {
        Log.d(TAG, "正在连接设备: ${device.address}")

        bluetoothGatt = device.connectGatt(
            context,
            false,
            object : BluetoothGattCallback() {

                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            Log.d(TAG, "已连接，请求MTU")
                            gatt.requestMtu(512)
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.d(TAG, "设备已断开")
                            gatt.close()
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                }

                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "MTU已更改为: $mtu")
                    }
                    gatt.discoverServices()
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "服务发现成功")

                        val service = gatt.getService(BLEModule.SERVICE_UUID)
                        if (service != null) {
                            Log.d(TAG, "找到目标服务")
                            continuation.resume(gatt)
                        } else {
                            Log.e(TAG, "未找到目标服务")
                            gatt.close()
                            continuation.resume(null)
                        }
                    } else {
                        Log.e(TAG, "服务发现失败: $status")
                        gatt.close()
                        continuation.resume(null)
                    }
                }

                // 处理写入回调
                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    Log.d(TAG, "写入回调: status=$status")

                    val success = status == BluetoothGatt.GATT_SUCCESS

                    writeContinuation?.let { cont ->
                        if (cont.isActive) {
                            cont.resume(success)
                        }
                    }
                    writeContinuation = null
                }


                // 处理 Notification 数据
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray
                ) {
                    Log.d(TAG, "📥 收到 Notification: ${value.size} 字节")

                    // 追加数据
                    receivedData.write(value)

                    Log.d(TAG, "累计接收: ${receivedData.size()} 字节")

                    // 第一阶段：接收 JSON
                    if (!jsonReceived) {
                        val jsonString = String(receivedData.toByteArray(), Charsets.UTF_8)
                        if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                            try {
                                JSONObject(jsonString)

                                // 解析成功
                                profileTransfer =
                                    json.decodeFromString<UserProfileTransfer>(jsonString)
                                jsonReceived = true

                                Log.d(TAG, "✅ JSON 接收完成: ${receivedData.size()} 字节")

                                Log.d(TAG, "等待接收头像")
                                receivedData.reset()  // 清空缓冲区，准备接收头像
                            } catch (_: Exception) {
                                Log.d(TAG, "JSON 未完成，继续接收...")
                            }
                        }
                    }
                    // 第二阶段：接收头像二进制
                    else {
                        val expectedSize = profileTransfer?.avatarSize ?: 0

                        if (receivedData.size() >= expectedSize) {
                            val avatarBytes = receivedData.toByteArray()

                            Log.d(TAG, "✅ 头像接收完成: ${avatarBytes.size} 字节")

                            // 返回 JSON + 头像
                            readContinuation?.let { cont ->
                                if (cont.isActive) {
                                    cont.resume(Pair(profileTransfer!!, avatarBytes))
                                }
                            }
                            readContinuation = null
                            reset()
                        }
                    }
                }
            }
        )
    }

    private fun reset() {
        receivedData.reset()
        jsonReceived = false
        profileTransfer = null
    }

    /**
     * 订阅 Notification 并读取数据
     * 返回 Pair<JSON, 头像字节数组>
     */
    @SuppressLint("MissingPermission")
    suspend fun readProfile(gatt: BluetoothGatt): Pair<UserProfileTransfer, ByteArray?>? {
        return suspendCancellableCoroutine { continuation ->

            val service = gatt.getService(BLEModule.SERVICE_UUID)
            val characteristic = service?.getCharacteristic(BLEModule.CHARACTERISTIC_UUID)

            if (characteristic == null) {
                Log.e(TAG, "未找到特征")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            reset()
            readContinuation = continuation

            // 启用 Notification
            val notificationEnabled = gatt.setCharacteristicNotification(characteristic, true)

            if (!notificationEnabled) {
                Log.e(TAG, "启用 Notification 失败")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            // 写入 Descriptor 以订阅
            val descriptor = characteristic.getDescriptor(BLEModule.DESCRIPTOR_UUID)
            if (descriptor != null) {
                val result = writeDescriptorCompat(
                    gatt,
                    descriptor
                )

                if (result) {
                    Log.d(TAG, "✅ 已订阅 Notification，等待数据...")
                } else {
                    Log.e(TAG, "写入 Descriptor 失败")
                    continuation.resume(null)
                }
            } else {
                Log.e(TAG, "未找到 Descriptor")
                continuation.resume(null)
            }

            // 30秒超时
            CoroutineScope(Dispatchers.IO).launch {
                delay(30000)
                if (readContinuation == continuation && continuation.isActive) {
                    Log.e(TAG, "读取超时")
                    readContinuation = null
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * 兼容新旧 API 的 Descriptor 写入
     */
    @SuppressLint("MissingPermission")
    private fun writeDescriptorCompat(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun close() {
        readContinuation?.cancel()
        readContinuation = null
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}