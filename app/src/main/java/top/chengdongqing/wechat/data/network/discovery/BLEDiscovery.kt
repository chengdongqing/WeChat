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
import org.json.JSONObject
import top.chengdongqing.wechat.data.network.service.P2PService
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume

class BLEDiscovery @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    // 保存读取回调的 continuation
    private var readContinuation: CancellableContinuation<String?>? = null

    private val receivedData = ByteArrayOutputStream()

    companion object {
        private const val TAG = "BLEDiscovery"
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
                    .setServiceUuid(ParcelUuid(P2PService.SERVICE_UUID))
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
                            ParcelUuid(P2PService.SERVICE_UUID)
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

                        val service = gatt.getService(P2PService.SERVICE_UUID)
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

                    // 尝试解析（判断是否完整）
                    val jsonString = String(receivedData.toByteArray(), Charsets.UTF_8)
                    if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                        try {
                            // 验证JSON是否完整
                            JSONObject(jsonString)

                            Log.d(TAG, "✅ 接收完成: ${receivedData.size()} 字节")

                            // 只 resume 一次
                            readContinuation?.let { cont ->
                                if (cont.isActive) {
                                    cont.resume(jsonString)
                                }
                            }
                            readContinuation = null
                        } catch (_: Exception) {
                            // JSON不完整，继续接收
                            Log.d(TAG, "JSON未完成，继续接收...")
                        }
                    }
                }
            }
        )
    }

    /**
     * 订阅 Notification 并读取数据
     */
    @SuppressLint("MissingPermission")
    suspend fun readProfile(gatt: BluetoothGatt): String? {
        return suspendCancellableCoroutine { continuation ->

            val service = gatt.getService(P2PService.SERVICE_UUID)
            val characteristic = service?.getCharacteristic(P2PService.CHARACTERISTIC_UUID)

            if (characteristic == null) {
                Log.e(TAG, "未找到特征")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            receivedData.reset()
            readContinuation = continuation

            // 启用 Notification
            val notificationEnabled = gatt.setCharacteristicNotification(characteristic, true)

            if (!notificationEnabled) {
                Log.e(TAG, "启用 Notification 失败")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            // 写入 Descriptor 以订阅
            val descriptor = characteristic.getDescriptor(P2PService.DESCRIPTOR_UUID)
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