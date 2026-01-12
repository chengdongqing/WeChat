package top.chengdongqing.wechat.data.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import kotlinx.coroutines.withTimeout
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
import java.util.concurrent.ConcurrentHashMap

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
    private val deviceId: String by lazy { IdManager(context).getDeviceId() }
    private val deviceName by lazy { context.getDeviceName() }

    // 消息调度器
    private val messageDispatcher = ServiceLocator.getMessageDispatcher(context)

    // 消息接收服务
    private var messageServerJob: Job? = null

    @SuppressLint("MissingPermission")
    override fun startDiscovery() {
        stopDiscovery()
        makeDiscoverable()

        // 注册广播监听
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_UUID)
        }
        context.registerReceiver(bluetoothReceiver, filter)

        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        // 开始扫描设备
        bluetoothAdapter?.startDiscovery()
    }

    /**
     * 启用蓝牙发现
     */
    private fun makeDiscoverable(seconds: Int = 30) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 定义广播接收器
     */
    @SuppressLint("MissingPermission")
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            when (intent.action) {
                // 当发现设备
                BluetoothDevice.ACTION_FOUND -> {
                    // 发送获取uuid的请求
                    device?.fetchUuidsWithSdp()
                }
                // 当uuid回调
                BluetoothDevice.ACTION_UUID -> {
                    val uuids = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayExtra(
                            BluetoothDevice.EXTRA_UUID,
                            ParcelUuid::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                    }

                    uuids?.forEach { uuid ->
                        // 判断是否符合协议
                        if (uuid.toString().equals(appUUID.toString(), ignoreCase = true)) {
                            device?.name?.let {
                                scope.launch {
                                    resolveIdentity(device)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val getIdentityCode = "GET_IDENTITY"

    // 存储正在解析中的设备，防止广播多次触发导致重复连接
    private val resolvingDevices = ConcurrentHashMap<String, Boolean>()

    /**
     * 获取设备身份信息
     */
    @SuppressLint("MissingPermission")
    private suspend fun resolveIdentity(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        if (resolvingDevices[device.address] == true) {
            return@withContext
        }

        try {
            println("---resolveIdentity-1")
            // 尝试连接对方的监听端口
            device.createInsecureRfcommSocketToServiceRecord(appUUID).use { socket ->
                println("---resolveIdentity-2")
                // 设置连接超时，不能让扫描卡死
                withTimeout(3000) {
                    socket.connect()
                    println("---resolveIdentity-3")
                    val output = socket.outputStream
                    val input = socket.inputStream

                    // 发送请求指令
                    val request = "$getIdentityCode\n"
                    output.write(request.toByteArray())
                    output.flush()
                    println("---resolveIdentity-4")

                    // 读取对方返回的数据
                    val text = input.readRawLine() ?: return@withTimeout

                    println("----parseProtocol:$text")

                    // 数据解析
                    if (parseProtocol(text, device)) {
                        resolvingDevices[device.address] = true
                    }

                    output.write(1)
                }
            }
        } catch (_: Exception) {
            Log.d("BT", "身份解析失败: ${device.address}")
        }
    }

    /**
     * 解析协议
     */
    private fun parseProtocol(text: String, device: BluetoothDevice): Boolean {
        val peer = super.decodeIdentity(text) ?: return false

        _peers.upsert(
            BluetoothPeer(
                peer.id,
                peer.name,
                device.address,
                device.bondState == BluetoothDevice.BOND_BONDED,
            )
        ) { it.id }
        return true
    }

    override suspend fun sendText(peer: P2PPeer, envelope: MessageEnvelope): Boolean =
        withContext(Dispatchers.IO) {
            val btPeer = peer as? BluetoothPeer ?: return@withContext false
            val content = AppJson.instance.encodeToString(envelope) + "\n"

            performSend(btPeer.mac, content.toByteArray(), null, null)
        }

    override suspend fun sendMedia(
        peer: P2PPeer,
        envelope: MessageEnvelope,
        file: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val btPeer = peer as? BluetoothPeer ?: return@withContext false
        val header = AppJson.instance.encodeToString(envelope) + "\n"

        performSend(btPeer.mac, header.toByteArray(), file, onProgress)
    }

    /**
     * 处理发送逻辑
     */
    private suspend fun performSend(
        address: String,
        header: ByteArray,
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
                output.write(header)
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
                bluetoothAdapter?.listenUsingRfcommWithServiceRecord("WeChat", appUUID)
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

            println("---handleIncomingConnection:$headerLine")

            // 发送身份信息
            if (headerLine == getIdentityCode) {
                val identity = super.encodeIdentity(deviceId, deviceName)
                output.write(identity.toByteArray())
                output.flush()

                // 等待回复接收完毕 (ACK)
                withTimeoutOrNull(5000) {
                    input.read()
                } != -1
                return@use
            }

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
            context.unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {
        }
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
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