package top.chengdongqing.wechat.data.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeText
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.AppJson
import top.chengdongqing.wechat.core.util.IdManager
import top.chengdongqing.wechat.core.util.ServiceLocator
import top.chengdongqing.wechat.core.util.getDeviceName
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import top.chengdongqing.wechat.data.model.WifiLanPeer
import java.io.File
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

/**
 * Wi-Fi局域网内通信。Lan：局域网（Local Area Network）
 * 这里包含基于UDP的服务广播/发现、基于TCP的消息发送/接收等网络处理
 *
 * 关于单播、广播和组播：
 * 1.单播 (Unicast): 你给某个死党打电话。只有你们两个在通话。
 * 2.广播 (Broadcast): 你拿着大喇叭在学校操场喊：“大家来开会！”。不管想不想听，全校师生（局域网内所有设备）都会被迫收到这条信息。缺点： 吵（浪费带宽和 CPU 资源），且很多现代路由器（如公共商场、学校 WiFi）出于安全考虑会直接封杀这种“大喇叭”。
 * 3.组播 (Multicast): 你创建了一个特定的“兴趣小组”。只有加入了这个小组的人才能听到。
 */
class WifiLanManager(private val context: Context) : AbstractP2pManger(), P2pConnectionManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val selectorManager = SelectorManager(Dispatchers.IO)

    private val _peers = MutableStateFlow<List<P2PPeer>>(emptyList())
    override val peers: StateFlow<List<P2PPeer>> = _peers

    // 设备信息
    private val deviceId: String by lazy { IdManager(context).getDeviceId() }
    private val deviceName by lazy { context.getDeviceName() }

    // 消息调度器
    private val messageDispatcher = ServiceLocator.getMessageDispatcher(context)

    // 设备扫描任务
    private var discoveryJob: Job? = null

    // 消息接收服务
    private var messageServerJob: Job? = null

    // 发送数据包的间隔
    private val discoveryInterval = 3000L

    // 自定义组播地址
    private val multicastIp = "239.10.10.10"
    private val multicastPort = 9999
    private val messagePort = 8888

    override fun startDiscovery() {
        // 权限与状态前置检查
        ensureMulticastLock()
        stopDiscovery()

        discoveryJob = scope.launch(Dispatchers.IO) {
            // 发送广播
            launch { runMulticastSender() }
            // 搜索服务
            launch { runMulticastReceiver() }
        }
    }

    private suspend fun runMulticastSender() {
        val sendSocket = aSocket(selectorManager).udp().bind {
            broadcast = true
        }
        val groupAddress = InetSocketAddress(multicastIp, multicastPort)
        // 定义协议信息并作为数据包的内容
        val identity = super.encodeIdentity(deviceId, deviceName)

        // 每3秒发送一次
        sendSocket.use { socket ->
            while (currentCoroutineContext().isActive) {
                try {
                    // 构建数据包
                    val datagram = Datagram(buildPacket { writeText(identity) }, groupAddress)
                    // 发送数据包
                    socket.send(datagram)
                } catch (e: Exception) {
                    Log.e("UDP", "Send failed", e)
                }
                delay(discoveryInterval)
            }
        }
    }

    private suspend fun runMulticastReceiver() {
        val receiveSocket = MulticastSocket(multicastPort).apply {
            reuseAddress = true
        }
        val groupAddress = InetAddress.getByName(multicastIp)

        // 持续监听所有数据包
        receiveSocket.use { socket ->
            // 加入组播（订阅指定频道）
            socket.joinGroup(groupAddress)
            // 数据缓冲池
            val buffer = ByteArray(1024)

            try {
                while (currentCoroutineContext().isActive) {
                    // 接收数据包（会阻塞直到收到包）
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    // 数据解码
                    val text = packet.data.decodeToString(0, packet.length)
                    val remoteIp = packet.address.hostAddress ?: ""
                    // 数据解析
                    parseProtocol(text, remoteIp)
                }
            } catch (e: Exception) {
                Log.e("UDP", "Receive failed", e)
            } finally {
                // 通知路由器停止转发，避免流量浪费或下次不可预知的冲突
                socket.leaveGroup(groupAddress)
            }
        }
    }

    /**
     * 解析协议
     */
    private fun parseProtocol(text: String, remoteIp: String) {
        val peer = super.decodeIdentity(text) ?: return
        // 过滤自己发的数据包
        if (peer.id != deviceId) {
            _peers.upsert(WifiLanPeer(peer.id, peer.name, remoteIp)) { it.id }
        }
    }

    /**
     * 组播锁
     */
    private val multicastLock by lazy {
        (context.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createMulticastLock("p2p_wifi_lock")
    }

    /**
     * 申请组播锁，确保设备能够发送和接收广播数据
     * 为了省电，系统默认可能会拦截组播/广播包
     */
    private fun ensureMulticastLock() {
        if (!multicastLock.isHeld) {
            multicastLock.acquire()
        }
    }

    /**
     * 释放组播锁
     */
    private fun releaseMulticastLock() {
        if (multicastLock.isHeld) {
            multicastLock.release()
        }
    }


    override fun stopDiscovery() {
        discoveryJob?.cancel()
        releaseMulticastLock()
    }

    override suspend fun sendText(peer: P2PPeer, envelope: MessageEnvelope): Boolean {
        val lanPeer = peer as? WifiLanPeer ?: return false
        return performSend(lanPeer.ip, envelope)
    }

    override suspend fun sendMedia(
        peer: P2PPeer,
        envelope: MessageEnvelope,
        file: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean {
        val lanPeer = peer as? WifiLanPeer ?: return false
        return performSend(lanPeer.ip, envelope, file, onProgress)
    }

    /**
     * 处理发送逻辑
     */
    private suspend fun performSend(
        ip: String,
        envelope: MessageEnvelope,
        file: File? = null,
        onProgress: (suspend (Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val header = AppJson.instance.encodeToString(envelope) + "\n"

        try {
            aSocket(selectorManager).tcp().connect(ip, messagePort).use { socket ->
                val writeChannel = socket.openWriteChannel(autoFlush = false)

                // 发送 Header
                writeChannel.writeStringUtf8(header)
                writeChannel.flush()

                // 如果有文件，发送 Body (Binary)
                file?.let {
                    val fileLength = it.length()
                    var totalSent = 0L
                    val buffer = ByteArray(1024 * 256) // 256KB

                    it.inputStream().use { input ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break

                            writeChannel.writeFully(buffer, 0, read)
                            totalSent += read

                            // 刷新缓冲区并回调进度
                            writeChannel.flush()
                            onProgress?.invoke(totalSent.toFloat() / fileLength)
                        }
                    }
                } ?: writeChannel.flush() // 纯文本模式下确保 Header 发出

                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("WifiLanManager", "TCP Send failed to $ip", e)
            return@withContext false
        }
    }

    override fun startMessageServer() {
        stopMessageServer()

        messageServerJob = scope.launch(Dispatchers.IO) {
            // 开启一个TCP Socket：0.0.0.0代表监听本地所有类型的网卡（如Wi-Fi、蜂窝网络等）
            val socketAddress = InetSocketAddress("0.0.0.0", messagePort)
            val serverSocket = try {
                aSocket(selectorManager).tcp().bind(socketAddress) {
                    reuseAddress = true
                }
            } catch (e: Exception) {
                // 这里负责处理“端口被占用”或“网卡不可用”等致命错误
                Log.e("WifiLanManager", "无法创建监听端口", e)
                return@launch
            }

            serverSocket.use { socket ->
                while (isActive) {
                    val clientSocket = try {
                        socket.accept() // 阻塞等待
                    } catch (e: Exception) {
                        if (isActive) {
                            // 捕获 accept 或协程启动错误
                            Log.e("WifiLanManager", "Server accept error", e)
                            delay(200) // 避免因权限等持续性错误导致的“死循环闪退”
                            continue // 继续下一次 accept
                        } else break
                    }

                    // 每一个新的连接都开启一个独立协程处理
                    launch { clientSocket.handleClientConnection() }
                }
            }
        }
    }

    /**
     * 处理客户端连接
     */
    private suspend fun Socket.handleClientConnection() = this.use { socket ->
        val senderIp = (remoteAddress as? InetSocketAddress)?.hostname ?: ""

        try {
            // 打开一个读取数据的通道
            val readChannel = socket.openReadChannel()
            // 读取第一行
            val headerLine = readChannel.readUTF8Line() ?: return@use

            // 解析信封
            var envelope = try {
                AppJson.instance.decodeFromString<MessageEnvelope>(headerLine)
            } catch (e: Exception) {
                // 可能会有局域网内其它App误撞端口导致数据无法解析等
                Log.e("TCP", "JSON decode error: ${e.message}")
                return@use
            }.copy(senderIp = senderIp)

            // 处理文件流
            val payload = envelope.payload
            if (payload is ChatPayload.Media) {
                // 创建文件夹
                val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
                // 创建文件
                val file = File(mediaDir, "${envelope.id}_${payload.fileName}")
                // 拷贝数据到文件
                file.outputStream().use { output ->
                    readChannel.copyTo(output, payload.size)
                }
                // 更新文件路径
                envelope = envelope.copy(payload = payload.copy(localPath = file.absolutePath))
            }

            // 分发消息
            messageDispatcher.dispatch(envelope)
        } catch (e: Exception) {
            Log.e("TCP", "Handle client error: ${e.message}")
        }
    }

    override fun stopMessageServer() {
        messageServerJob?.cancel()
    }

    override suspend fun sendPayload(targetIp: String, payload: ChatPayload): Boolean {
        val envelope = MessageEnvelope(
            id = randomUUID(),
            senderId = deviceId,
            senderName = deviceName,
            payload = payload
        )

        return sendText(WifiLanPeer(id = "", name = "", ip = targetIp), envelope)
    }

    override suspend fun connect(peer: P2PPeer): Boolean = true
    override fun disconnect(peer: P2PPeer) {}
}