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
import top.chengdongqing.wechat.data.model.BluetoothPeer
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import java.io.File
import java.io.InputStream
import java.util.UUID

class BluetoothManager(private val context: Context) : P2pConnectionManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager?)?.adapter
    }

    // 蓝牙通信协议的唯一标识 (UUID)
    private val appUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP服务

    private val _peers = MutableStateFlow<List<P2PPeer>>(emptyList())
    override val peers: StateFlow<List<P2PPeer>> = _peers

    private var serverJob: Job? = null

    // 获取唯一设备ID，防止发现自己
    private val myId: String by lazy { IdManager(context).getMyId() }

    // 1. 定义广播接收器
    @SuppressLint("MissingPermission")
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            println("----BT_LOG: 收到广播 Action = ${intent.action}")

            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    println("----BT_LOG: 发现设备 -> ${device?.name} | ${device?.address} | id:${device?.uuids}")
                    device?.fetchUuidsWithSdp()
                }

                BluetoothDevice.ACTION_UUID -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val uuids = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayExtra(
                            BluetoothDevice.EXTRA_UUID,
                            ParcelUuid::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                    }

                    println("----uuids:${uuids}")

                    uuids?.forEach { uuid ->
                        if (uuid.toString().equals(appUUID.toString(), ignoreCase = true)) {
                            println("----BT_LOG: 确认过眼神，是装了 App 的设备: ${device?.name},id:${device?.uuids}")
                            device?.let { updatePeerList(it) }
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // 搜索结束逻辑
                }
            }
        }
    }

    private fun makeDiscoverable() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // 注意：这需要 Context 启动 Activity，建议在 UI 层触发
        context.startActivity(discoverableIntent)
    }

    // --- 1. 发现逻辑 ---
    @SuppressLint("MissingPermission")
    override fun startDiscovery(deviceName: String) {
        makeDiscoverable()
        // 开启监听服务
        startMessageServer()

        // 扫描逻辑 (此处简化，通常使用 BluetoothLeScanner 或 BroadcastReceiver 监听搜索结果)
        // 蓝牙搜索到设备后调用 updatePeerList(BluetoothPeer(...))
        // 注册广播监听搜寻结果
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_UUID)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)

        // 3. 必须先取消之前的扫描，否则第二次启动必报 false
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        val success = bluetoothAdapter?.startDiscovery() ?: false
        println("----BT_LOG: 启动扫描是否成功: $success")

        // 4. 先把已经配对的设备填进去，这样不用搜也能看到
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            updatePeerList(device)
        }
    }

    // --- 2. 消息发送逻辑 (Client) ---
    override suspend fun sendText(peer: P2PPeer, envelope: MessageEnvelope): Boolean {
        val btPeer = peer as? BluetoothPeer ?: return false
        val content = AppJson.instance.encodeToString(envelope)
        return performBluetoothWrite(btPeer.mac, content.toByteArray(), null)
    }

    override suspend fun sendMedia(
        peer: P2PPeer,
        envelope: MessageEnvelope,
        file: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean {
        val btPeer = peer as? BluetoothPeer ?: return false
        val header = AppJson.instance.encodeToString(envelope) + "\n"
        return performBluetoothWrite(btPeer.mac, header.toByteArray(), file, onProgress)
    }

    // 核心写操作：建立 Socket -> 发 Header -> 发文件
    private suspend fun performBluetoothWrite(
        address: String,
        header: ByteArray,
        file: File?,
        onProgress: (suspend (Float) -> Unit)? = null
    ): Boolean {
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return false
        var socket: BluetoothSocket? = null
        return try {
            socket = device.createRfcommSocketToServiceRecord(appUUID)
            socket.connect()
            val output = socket.outputStream
            val input = socket.inputStream // 拿输入流准备收 ACK

            // 1. 发送 Header
            output.write(header)

            // 2. 发送二进制
            file?.let {
                it.inputStream().use { fileInput ->
                    val buffer = ByteArray(16384) // 蓝牙可以稍微加大点 buffer 提高效率
                    var totalSent = 0L
                    val fileLength = it.length()
                    while (true) {
                        val read = fileInput.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        totalSent += read
                        onProgress?.invoke(totalSent.toFloat() / fileLength)
                    }
                }
            }
            output.flush()

            // 3. --- 关键：等待接收端的 ACK ---
            // 我们阻塞在这里，直到接收端写回一个字节，或者连接断开
            val ack = withTimeoutOrNull(10000) { // 设置 10 秒超时
                input.read()
            }

            ack != -1 // 如果读到 -1 说明还没收到 ACK 对方就断了
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            socket?.close()
        }
    }

    // --- 3. 消息接收逻辑 (Server) ---
    @SuppressLint("MissingPermission")
    override fun startMessageServer() {
        serverJob = scope.launch {
            val serverSocket =
                bluetoothAdapter?.listenUsingRfcommWithServiceRecord("P2P_Chat", appUUID)
            try {
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch {
                        handleIncomingConnection(socket)
                    }
                }
            } finally {
                serverSocket?.close()
            }
        }
    }

    override fun stopMessageServer() {
        serverJob?.cancel()
    }

    private suspend fun handleIncomingConnection(socket: BluetoothSocket) {
        try {
            val input = socket.inputStream
            val output = socket.outputStream

            // 1. 【修复】禁用 BufferedReader，改为手动读取字节直到换行符
            // 这样可以确保只读走 JSON，不碰后面的二进制流
            val headerLine = input.readRawLine() ?: return
            println("------headerLine:$headerLine")

            val envelope = AppJson.instance.decodeFromString<MessageEnvelope>(headerLine)

            // 2. 处理媒体文件
            if (envelope.payload is ChatPayload.Media) {
                val payload = envelope.payload
                val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
                val file = File(mediaDir, "bt_${envelope.id}_${payload.fileName}")

                file.outputStream().use { fileOut ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L
                    // 【修复】精确控制读取长度，不多读也不少读
                    while (totalRead < payload.size) {
                        val remaining =
                            (payload.size - totalRead).coerceAtMost(buffer.size.toLong()).toInt()
                        val read = input.read(buffer, 0, remaining)
                        if (read == -1) break
                        fileOut.write(buffer, 0, read)
                        totalRead += read
                    }
                }

                val updatedPayload = payload.copy(localPath = file.absolutePath)
//                _messageFlow.emit(envelope.copy(payload = updatedPayload))
            } else {
                // 3. 处理纯文本
//                _messageFlow.emit(envelope)
            }

            // 4. 【新增回执】所有数据处理完毕，告诉发送端：你可以安心断开了
            output.write(1)
            output.flush()

        } catch (e: Exception) {
            println("------蓝牙接收出错: ${e.message}")
            e.printStackTrace()
        } finally {
            // 给 ACK 一点点物理传输时间再关 Socket
            delay(200)
            socket.close()
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // 忽略未注册异常
        }
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        serverJob?.cancel()
        _peers.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(peer: P2PPeer): Boolean = withContext(Dispatchers.IO) {
        val btPeer = peer as? BluetoothPeer ?: return@withContext false
        val device =
            bluetoothAdapter?.getRemoteDevice(btPeer.mac) ?: return@withContext false

        return@withContext try {
            // 如果未配对，尝试发起配对请求
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                device.createBond()
                // 这里可以简单等待配对成功，或者让用户在系统弹窗确认
                false
            } else {
                true // 已配对，可以进行后续发送
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun disconnect(peer: P2PPeer) {}

    @SuppressLint("MissingPermission")
    private fun updatePeerList(device: BluetoothDevice) {
        val currentList = _peers.value.toMutableList()

        // 构造 Peer 对象，并包含配对状态
        val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
        val displayName =
            if (isBonded) "[已配对] ${device.name ?: "未知"}" else (device.name ?: device.address)

        val newPeer = BluetoothPeer(
            id = device.address,
            name = displayName,
            mac = device.address,
        )

        val index = currentList.indexOfFirst { it.id == newPeer.id }
        if (index == -1) {
            currentList.add(newPeer)
        } else {
            currentList[index] = newPeer
        }

        // 按配对状态排序：已配对的排在前面
        // _peers.value = currentList.sortedByDescending { it.name.contains("[已配对]") }
        _peers.value = currentList
    }
}

/**
 * 从输入流中读取一行，直到遇到换行符，且不使用缓冲区，避免干扰后续二进制流
 */
private suspend fun InputStream.readRawLine(): String? = withContext(Dispatchers.IO) {
    val bytes = mutableListOf<Byte>()
    while (true) {
        val b = read() // 阻塞式读取
        if (b == -1) return@withContext if (bytes.isEmpty()) null else String(bytes.toByteArray())
        if (b == '\n'.code) break
        bytes.add(b.toByte())
    }
    String(bytes.toByteArray(), Charsets.UTF_8).trim()
}