package top.chengdongqing.wechat.data.network

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.cio.use
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeText
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.readText
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
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
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import top.chengdongqing.wechat.data.model.WifiLanPeer
import top.chengdongqing.wechat.ui.call.CallActivity
import java.io.File

class WifiLanManager(private val context: Context) : P2pConnectionManager {

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val broadcastPort = 9999
    private val messagePort = 8888

    // 接口成员实现
    private val _peers = MutableStateFlow<List<P2PPeer>>(emptyList())
    override val peers: StateFlow<List<P2PPeer>> = _peers

    private val _messageFlow = MutableSharedFlow<MessageEnvelope>(
        replay = 1, // 缓存最近的一条，防止 Activity 启动慢导致漏掉第一个 Sdp
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val messageFlow: SharedFlow<MessageEnvelope> = _messageFlow

    private var discoveryJob: Job? = null
    private var serverJob: Job? = null

    // 获取唯一设备ID，防止发现自己
    private val myId: String by lazy { IdManager(context).getMyId() }

    private val multicastLock by lazy {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createMulticastLock("p2p_wifi_lock")
    }

    // --- 1. 发现逻辑 ---
    override fun startDiscovery(myName: String) {
        if (!multicastLock.isHeld) multicastLock.acquire() // 必须申请锁

        stopDiscovery()
        startMessageServer()

        discoveryJob = scope.launch {
            // --- 1. 发送逻辑 (Sender) ---
            launch {
                // 注意：这里 bind() 不带参数，让系统随机分配发送端口
                val sendSocket = aSocket(selectorManager).udp().bind {
                    broadcast = true
                }
                val broadcastAddr = InetSocketAddress("255.255.255.255", broadcastPort)

                try {
                    while (isActive) {
                        val protocolMsg = "P2P_HI|$myId|$myName"
                        sendSocket.send(
                            Datagram(
                                buildPacket { writeText(protocolMsg) },
                                broadcastAddr
                            )
                        )
                        delay(3000)
                    }
                } finally {
                    sendSocket.close()
                }
            }

            // --- 2. 接收逻辑 (Receiver) ---
            launch {
                // 只有监听端需要绑定到 9999 端口
                val receiveSocket = aSocket(selectorManager).udp()
                    .bind(InetSocketAddress("0.0.0.0", broadcastPort)) {
                        reuseAddress = true // 允许地址重用，防止退出后立即重启报错
                    }

                try {
                    while (isActive) {
                        val datagram = receiveSocket.receive()
                        val text = datagram.packet.readText()
                        // 注意：使用 hostString 避免触发反向 DNS 解析，性能更好
                        val remoteIp = (datagram.address as InetSocketAddress).hostname

                        if (text.startsWith("P2P_HI|")) {
                            val parts = text.split("|")
                            if (parts.size >= 3) {
                                val id = parts[1]
                                val name = parts[2]
                                if (id != myId) {
                                    updatePeerList(WifiLanPeer(id, name, remoteIp))
                                }
                            }
                        }
                    }
                } finally {
                    receiveSocket.close()
                }
            }
        }
    }

    private fun updatePeerList(newPeer: WifiLanPeer) {
        val current = _peers.value.toMutableList()
        val index = current.indexOfFirst { (it as WifiLanPeer).id == newPeer.id }
        if (index == -1) {
            current.add(newPeer)
        } else {
            current[index] = newPeer // 更新 IP
        }
        _peers.value = current
    }

    override fun stopDiscovery() {
        discoveryJob?.cancel()
        serverJob?.cancel()
        _peers.value = emptyList()
    }

    // --- 2. 消息发送逻辑 (TCP Client) ---
    override suspend fun sendText(peer: P2PPeer, text: String): Boolean {
        val lanPeer = peer as? WifiLanPeer ?: return false
        // 构造信封并序列化为 JSON
        val envelope = MessageEnvelope(
            id = randomUUID(),
            senderId = myId,
            senderName = "MyName", // 建议从传参拿
            payload = ChatPayload.Text(text),
            timestamp = System.currentTimeMillis()
        )
        val jsonStr = AppJson.instance.encodeToString(MessageEnvelope.serializer(), envelope)

        return try {
            val socket = aSocket(selectorManager).tcp().connect(lanPeer.ip, messagePort)
            val writeChannel = socket.openWriteChannel(autoFlush = true)

            // 记得加 \n，因为接收端用的是 readUTF8Line()
            writeChannel.writeStringUtf8(jsonStr + "\n")
            writeChannel.flushAndClose()

            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 模拟发送文件的核心逻辑
    override suspend fun sendMedia(
        peer: P2PPeer,
        payload: ChatPayload.Media,
        file: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean {
        val lanPeer = peer as? WifiLanPeer ?: return false

        // 构造信封并序列化为 JSON
        val envelope = MessageEnvelope(
            id = randomUUID(),
            senderId = myId,
            senderName = "MyName", // 建议从传参拿
            payload = payload,
            timestamp = System.currentTimeMillis()
        )

        return try {
            val socket = aSocket(selectorManager).tcp().connect(lanPeer.ip, messagePort)
            val writeChannel = socket.openWriteChannel(autoFlush = false)
            // 1. 发送 JSON Header
            val header = AppJson.instance.encodeToString(envelope) + "\n"
            writeChannel.writeStringUtf8(header)
            writeChannel.flush()

            // 3. 直接将文件流写入 Channel (Ktor 提供的高效方式)
            file.inputStream().use { input ->
                val buffer = ByteArray(1024 * 8)
                var totalSent = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    writeChannel.writeFully(buffer, 0, read)
                    totalSent += read
                    onProgress(totalSent.toFloat() / file.length())
                    writeChannel.flush()
                }
            }

            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private var serverSocket: ServerSocket? = null // 增加引用

    // --- 3. 消息接收服务器 (TCP Server) ---
    private fun startMessageServer() {
        // 1. 启动前强制清理旧的
        serverSocket?.close()
        serverJob?.cancel()

        serverJob = scope.launch {
            serverSocket =
                aSocket(selectorManager).tcp().bind(InetSocketAddress("0.0.0.0", messagePort)) {
                    reuseAddress = true
                }

            try {
                val currentSocket = serverSocket!! // 局部变量防止并发问题
                while (isActive) {
                    val clientSocket = currentSocket.accept()
                    val remoteAddress = clientSocket.remoteAddress as? InetSocketAddress
                    val senderIp = remoteAddress?.hostname ?: "" // 这就是对方的 IP

                    println("-------senderIp:$senderIp")

                    launch {
                        try {
                            val readChannel = clientSocket.openReadChannel()

                            // 1. 只读取第一行（JSON Header）
                            // 注意：发送端必须使用 channel.writeStringUtf8(json + "\n")
                            val headerLine = readChannel.readUTF8Line() ?: return@launch
                            println("------headerLine:$headerLine")

                            val envelope =
                                AppJson.instance.decodeFromString<MessageEnvelope>(headerLine)
                            val payload = envelope.payload

                            when (payload) {
                                is ChatPayload.Media -> {
                                    // 2. 处理媒体文件
                                    // 建议在外部 filesDir 创建 media 文件夹，cacheDir 容易被系统清理
                                    val mediaDir =
                                        File(context.filesDir, "media").apply { mkdirs() }
                                    // 为了防止重名，可以使用 envelope.id 作为文件名的一部分
                                    val file =
                                        File(mediaDir, "p2p_${envelope.id}_${payload.fileName}")

                                    file.outputStream().use { output ->
                                        // 3. 关键：将 readChannel 中剩余的字节流直接拷贝到文件
                                        // copyTo 会持续读取直到发送端关闭 socket
                                        readChannel.copyTo(output)
                                    }

                                    // 更新 payload 指向新落地的本地路径
                                    val updatedPayload = payload.copy(localPath = file.absolutePath)
                                    _messageFlow.emit(envelope.copy(payload = updatedPayload))
                                }

                                is ChatPayload.CallAction if payload.action == "START_VIDEO" -> {
                                    println("----信令: 收到呼叫请求，正在拉起通话界面")

                                    val intent = Intent(context, CallActivity::class.java).apply {
                                        // 1. 必须加这个 Flag，因为是从 Service/Background Context 启动 Activity
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                        // 2. 传递参数：对方是谁，我是被叫方
                                        putExtra("targetIp", senderIp)
                                        putExtra("isOfferer", false)
                                    }
                                    context.startActivity(intent)

                                    // 同时也发给 Flow，方便聊天界面记录“通话已开始”
                                    _messageFlow.emit(envelope)
                                }

                                else -> {
                                    // 4. 普通文本消息
                                    _messageFlow.emit(envelope)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            clientSocket.close()
                        }
                    }
                }
            } finally {
                serverSocket?.close()
            }
        }
    }

    // 在 WifiLanManager 类中添加
    suspend fun sendPayload(targetIp: String, payload: ChatPayload): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val socket = aSocket(selectorManager).tcp().connect(targetIp, messagePort)
                // 构造外壳
                val envelope = MessageEnvelope(
                    id = randomUUID(),
                    senderId = myId,
                    senderName = "name",
                    payload = payload
                )
                // 序列化
                val json = AppJson.instance.encodeToString(envelope)

                socket.openWriteChannel(autoFlush = true).use {
                    writeStringUtf8(json + "\n")
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    override suspend fun connect(peer: P2PPeer): Boolean = true
    override fun disconnect(peer: P2PPeer) {}
}