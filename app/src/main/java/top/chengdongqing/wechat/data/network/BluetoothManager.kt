package top.chengdongqing.wechat.data.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.AppJson
import top.chengdongqing.wechat.core.util.IdManager
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.model.BluetoothPeer
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import java.io.File
import java.util.UUID

class BluetoothManager(private val context: Context) : P2pConnectionManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(BluetoothManager::class.java)
        manager?.bluetoothAdapter
    }

    // 蓝牙通信协议的唯一标识 (UUID)
    private val appUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP服务

    private val _peers = MutableStateFlow<List<P2PPeer>>(emptyList())
    override val peers: StateFlow<List<P2PPeer>> = _peers

    private val _messageFlow = MutableSharedFlow<MessageEnvelope>()
    override val messageFlow: SharedFlow<MessageEnvelope> = _messageFlow

    private var serverJob: Job? = null

    // 1. 定义广播接收器
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            println("----BT_LOG: 收到广播 Action = ${intent.action}")

            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                    println("----BT_LOG: 发现设备 -> ${device?.name} | ${device?.address}")
                    device?.let {
                        updatePeerList(it)
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
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // 注意：这需要 Context 启动 Activity，建议在 UI 层触发
        context.startActivity(discoverableIntent)
    }

    // --- 1. 发现逻辑 ---
    override fun startDiscovery(myName: String) {
        makeDiscoverable()
        // 开启监听服务
        startMessageServer()

        // 扫描逻辑 (此处简化，通常使用 BluetoothLeScanner 或 BroadcastReceiver 监听搜索结果)
        // 蓝牙搜索到设备后调用 updatePeerList(BluetoothPeer(...))
        // 注册广播监听搜寻结果
        val filter = IntentFilter().apply {
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
    override suspend fun sendText(peer: P2PPeer, text: String): Boolean {
        val btPeer = peer as? BluetoothPeer ?: return false
        val envelope = MessageEnvelope(
            id = randomUUID(),
            senderId = IdManager(context).getMyId(),
            senderName = "MyName",
            payload = ChatPayload.Text(text),
            timestamp = System.currentTimeMillis()
        )
        val json = AppJson.instance.encodeToString(MessageEnvelope.serializer(), envelope) + "\n"

        return performBluetoothWrite(btPeer.macAddress, json.toByteArray(), null)
    }

    override suspend fun sendMedia(
        peer: P2PPeer,
        payload: ChatPayload.Media,
        file: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean {
        val btPeer = peer as? BluetoothPeer ?: return false
        val envelope = MessageEnvelope(
            id = randomUUID(),
            senderId = IdManager(context).getMyId(),
            senderName = "MyName",
            payload = payload,
            timestamp = System.currentTimeMillis()
        )
        val header = AppJson.instance.encodeToString(MessageEnvelope.serializer(), envelope) + "\n"

        return performBluetoothWrite(btPeer.macAddress, header.toByteArray(), file, onProgress)
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

            // 1. 发送 Header
            output.write(header)
            output.flush()

            // 2. 如果有文件，发送二进制
            file?.let {
                val buffer = ByteArray(8192)
                var totalSent = 0L
                val fileLength = it.length()
                it.inputStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        totalSent += read
                        onProgress?.invoke(totalSent.toFloat() / fileLength)
                    }
                }
            }
            output.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            socket?.close()
        }
    }

    // --- 3. 消息接收逻辑 (Server) ---
    private fun startMessageServer() {
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

    private suspend fun handleIncomingConnection(socket: BluetoothSocket) {
        try {
            val input = socket.inputStream
            val reader = input.bufferedReader()

            // 1. 读取单行 JSON Header
            val headerLine = reader.readLine() ?: return
            val envelope = AppJson.instance.decodeFromString<MessageEnvelope>(headerLine)

            if (envelope.payload is ChatPayload.Media) {
                val payload = envelope.payload
                val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
                val file = File(mediaDir, "bt_${envelope.id}_${payload.fileName}")

                // 2. 接下来的流全部写入文件
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L
                    while (totalRead < payload.size) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        totalRead += read
                        // 这里可以发射进度
                    }
                }
                val updatedPayload = payload.copy(localPath = file.absolutePath)
                _messageFlow.emit(envelope.copy(payload = updatedPayload))
            } else {
                _messageFlow.emit(envelope)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket.close()
        }
    }

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

    override suspend fun connect(peer: P2PPeer): Boolean = withContext(Dispatchers.IO) {
        val btPeer = peer as? BluetoothPeer ?: return@withContext false
        val device =
            bluetoothAdapter?.getRemoteDevice(btPeer.macAddress) ?: return@withContext false

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

    private fun updatePeerList(device: BluetoothDevice) {

        println("-----device:$device")

        val currentList = _peers.value.toMutableList()

        // 构造 Peer 对象，并包含配对状态
        val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
        val displayName =
            if (isBonded) "[已配对] ${device.name ?: "未知"}" else (device.name ?: device.address)

        val newPeer = BluetoothPeer(
            id = device.address,
            name = displayName,
            macAddress = device.address,
            isBonded = isBonded
        )

        val index = currentList.indexOfFirst { it.id == newPeer.id }
        if (index == -1) {
            currentList.add(newPeer)
        } else {
            currentList[index] = newPeer
        }

        println("-----currentList:$currentList")

        // 按配对状态排序：已配对的排在前面
        // _peers.value = currentList.sortedByDescending { it.name.contains("[已配对]") }
        _peers.value = currentList
    }
}