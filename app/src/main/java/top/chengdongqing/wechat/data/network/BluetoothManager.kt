package top.chengdongqing.wechat.data.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import top.chengdongqing.wechat.core.util.AppJson
import top.chengdongqing.wechat.core.util.IdManager
import top.chengdongqing.wechat.core.util.ServiceLocator
import top.chengdongqing.wechat.core.util.getDeviceName
import top.chengdongqing.wechat.data.model.BluetoothPeer
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * 蓝牙通信
 * 这里包含启用发现、搜索设备、连接设备、消息发送/接收等处理
 */
class BluetoothManager(private val context: Context) : AbstractP2pManger(), P2pConnectionManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    // 蓝牙通信协议的唯一标识
    private val appUUID = UUID.fromString("00001101-0000-1000-8000-00815F9B34FB")
    private val bufferSize = 1024 * 16 // 16KB

    private val _peers = MutableStateFlow<List<P2PPeer>>(emptyList())
    override val peers: StateFlow<List<P2PPeer>> = _peers

    // 设备信息
    private val deviceId: String by lazy { IdManager(context).getDeviceId().take(6) }
    private val deviceName by lazy { context.getDeviceName() }

    // 消息调度器
    private val messageDispatcher = ServiceLocator.getMessageDispatcher(context)

    // 消息接收服务
    private var messageServerJob: Job? = null

    @SuppressLint("MissingPermission")
    override fun startDiscovery() {
        stopDiscovery()
        startBleAdvertising() // 同时让自己也处于可发现状态

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(appUUID)).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner?.startScan(filters, settings, bleScanCallback)
    }

    @SuppressLint("MissingPermission")
    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val record = result.scanRecord ?: return
            val identityBytes = record.getManufacturerSpecificData(0x1234) ?: return
            val peer = super@BluetoothManager.decodeIdentity(String(identityBytes)) ?: return

            _peers.upsert(
                BluetoothPeer(
                    id = peer.id,
                    name = peer.name,
                    mac = device.address
                )
            ) { it.id }
        }
    }

    // 在类中添加
    private var bleAdvertiser: BluetoothLeAdvertiser? = null

    @SuppressLint("MissingPermission")
    private fun startBleAdvertising() {
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()

        // 1. 主广播包：只放 UUID，确保别人能过滤到你
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(appUUID))
            .build()

        // 2. 扫描响应包：放你的自定义 identity 数据
        val identityBytes = super.encodeIdentity(deviceId, deviceName).toByteArray(Charsets.UTF_8)
        val scanResponseData = AdvertiseData.Builder()
            .addManufacturerData(0x1234, identityBytes)
            .build()

        bleAdvertiser?.startAdvertising(
            settings,
            advertiseData,
            scanResponseData,
            advertiseCallback
        )
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d("BT", "BLE 广播启动成功")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d("BT", "BLE 广播启动失败,code:$errorCode")
        }
    }

    override suspend fun sendText(peer: P2PPeer, envelope: MessageEnvelope): Boolean =
        withContext(Dispatchers.IO) {
            val btPeer = peer as? BluetoothPeer ?: return@withContext false
            val content = AppJson.instance.encodeToString(envelope) + "\n"

            performSend(btPeer.mac, content, null, null)
        }

    override suspend fun sendMedia(
        peer: P2PPeer,
        envelope: MessageEnvelope,
        file: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val btPeer = peer as? BluetoothPeer ?: return@withContext false
        val header = AppJson.instance.encodeToString(envelope) + "\n"

        performSend(btPeer.mac, header, file, onProgress)
    }

    /**
     * 处理发送逻辑
     */
    private suspend fun performSend(
        address: String,
        header: String,
        file: File?,
        onProgress: (suspend (Float) -> Unit)?
    ): Boolean {
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return false

        return try {
            // 建立连接
            device.createRfcommSocketToServiceRecord(appUUID).use { socket ->
                socket.connect()
                val output = socket.outputStream
                val input = socket.inputStream

                // 发送 Header
                output.write(header.toByteArray())
                output.flush()

                // 如果有文件，发送 Body (Binary)
                file?.let { it ->
                    val totalSize = it.length()
                    var sentSize = 0L
                    it.inputStream().use { fileIn ->
                        val buffer = ByteArray(bufferSize)
                        var read: Int
                        while (fileIn.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)

                            sentSize += read
                            val progress = sentSize.toFloat() / totalSize
                            onProgress?.invoke(progress)
                        }
                    }
                }
                output.flush()

                // 等待回复接收完毕 (ACK)
                withTimeoutOrNull(5000) {
                    input.read()
                } != -1
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Write failed to $address", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun startMessageServer() {
        stopMessageServer()

        messageServerJob = scope.launch(Dispatchers.IO) {
            // 注册蓝牙 SDP 服务并开启监听，利用 UUID 匹配机制实现 P2P 握手
            val serverSocket = try {
                bluetoothAdapter?.listenUsingRfcommWithServiceRecord(super.protocolPrefix, appUUID)
            } catch (e: Exception) {
                Log.e("BluetoothManager", "无法创建监听端口", e)
                return@launch
            }

            serverSocket?.use { server ->
                while (isActive) {
                    val clientSocket = try {
                        server.accept() // 阻塞等待
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e("BluetoothManager", "接受连接失败，尝试继续监听...", e)
                            delay(200) // 稍作停顿，防止硬件故障导致的死循环 CPU 占用
                            continue // 继续下一次 accept
                        } else break
                    }

                    // 每一个新的连接都开启一个独立协程处理
                    launch { clientSocket.handleIncomingConnection() }
                }
            }
        }
    }

    private suspend fun BluetoothSocket.handleIncomingConnection() = this.use { s ->
        try {
            val input = s.inputStream
            val output = s.outputStream

            // 读取 Header
            val headerLine = input.readRawLine() ?: return@use
            var envelope = AppJson.instance.decodeFromString<MessageEnvelope>(headerLine)

            // 处理媒体文件
            if (envelope.payload is ChatPayload.Media) {
                val payload = envelope.payload
                val file = File(
                    File(context.filesDir, "media").apply { mkdirs() },
                    "bt_${envelope.id}_${payload.fileName}"
                )

                file.outputStream().use { fileOut ->
                    val buffer = ByteArray(bufferSize)
                    var totalRead = 0L
                    while (totalRead < payload.size) {
                        val remaining =
                            (payload.size - totalRead).coerceAtMost(buffer.size.toLong()).toInt()
                        val read = input.read(buffer, 0, remaining)
                        if (read == -1) break
                        fileOut.write(buffer, 0, read)
                        totalRead += read
                    }
                }
                envelope = envelope.copy(payload = payload.copy(localPath = file.absolutePath))
            }

            // 分发消息
            messageDispatcher.dispatch(envelope)

            // 回传 ACK
            output.write(1)
            output.flush()
            delay(100) // 确保硬件缓冲区发出
        } catch (e: Exception) {
            Log.e("BT", "Handle incoming failed", e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        try {
            // 2. 停止经典蓝牙的搜索 (Discovery)
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }

            // 3. 停止 BLE 扫描 (重点)
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            // 这里的 bleScanCallback 必须是 startDiscovery 中使用的同一个实例
            scanner?.stopScan(bleScanCallback)

            // 4. 停止 BLE 广播 (可选)
            // 注意：如果你希望别人还能搜到你，就不停广播；
            // 如果想彻底“隐身”或者退出发现页，就停止。
            bleAdvertiser?.stopAdvertising(advertiseCallback)

            Log.d("BT", "Discovery and Advertising fully stopped.")
        } catch (e: Exception) {
            Log.e("BT", "Error while stopping discovery", e)
        }
    }

    override fun stopMessageServer() {
        messageServerJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(peer: P2PPeer): Boolean {
        val device = bluetoothAdapter?.getRemoteDevice((peer as BluetoothPeer).mac) ?: return false

        return if (device.bondState == BluetoothDevice.BOND_NONE) {
            // 请求配对
            device.createBond()
        } else {
            true
        }
    }

    override suspend fun sendPayload(targetIp: String, payload: ChatPayload): Boolean = false
    override fun disconnect(peer: P2PPeer) {}
}

/**
 * 从输入流中读取一行，直到遇到换行符
 * 不使用缓冲区，避免干扰后续二进制流
 */
private fun InputStream.readRawLine(): String? {
    val bytes = mutableListOf<Byte>()
    while (true) {
        val b = read() // 阻塞式读取
        if (b == -1) {
            return if (bytes.isEmpty()) {
                null
            } else {
                String(bytes.toByteArray())
            }
        }
        if (b == '\n'.code) break
        bytes.add(b.toByte())
    }
    return String(bytes.toByteArray(), Charsets.UTF_8).trim()
}