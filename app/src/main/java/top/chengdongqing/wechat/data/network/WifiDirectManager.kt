package top.chengdongqing.wechat.data.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.AppJson
import top.chengdongqing.wechat.core.util.IdManager
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.data.model.P2PPeer
import top.chengdongqing.wechat.data.model.WifiDirectPeer
import java.io.File

class WifiDirectManager(private val context: Context) : AbstractP2pManger(), P2pConnectionManager {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? =
        manager?.initialize(context, context.mainLooper, null)

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val messagePort = 8888
    private val myId: String by lazy { IdManager(context).getDeviceId() }

    // 状态管理
    private val _peers = MutableStateFlow<List<P2PPeer>>(emptyList())
    override val peers: StateFlow<List<P2PPeer>> = _peers

    private var serverJob: Job? = null
    private var isGo = false
    private var groupOwnerAddress: String? = null

    // --- 1. 生命周期与广播处理 ---

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    // 发现列表变动
                    manager?.requestPeers(channel) { peerList ->
                        _peers.value = peerList.deviceList.map { device ->
                            WifiDirectPeer(
                                device.deviceAddress,
                                device.deviceName,
                                device.deviceAddress
                            )
                        }
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    // 1. 不再获取 NetworkInfo，直接请求连接详情
                    manager?.requestConnectionInfo(channel) { info ->
                        if (info != null && info.groupFormed) {
                            // 组已经建立成功（无论是我是 GO 还是 GC）
                            println("-------WFD: 组已形成，准备处理连接逻辑")
                            handleConnectionInfo(info)
                        } else {
                            // 组不存在或已解散
                            println("-------WFD: 连接已断开或组已解散")
                            stopMessageServer()
                            // 清理 IP 缓存
                            isGo = false
                            groupOwnerAddress = null
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        // 关键：必须判断 groupFormed。有时广播触发了，但组还没建好，地址就是 null
        if (info.groupFormed) {
            groupOwnerAddress = info.groupOwnerAddress?.hostAddress
            isGo = info.isGroupOwner

            println("----连接信息更新: isGo=$isGo, GO_IP=$groupOwnerAddress")

            if (groupOwnerAddress != null) {
                startMessageServer()

                // 如果我是客户端 (GC)，我需要在这里立即向 GO 发送 Identity 包 (名片)
                // 因为 GO 此时并不知道我的 IP，只有我知道它的 IP。
                if (!isGo) {
                    scope.launch {
                        delay(1000) // 给 Server 一点启动时间
                        sendIdentityToGroupOwner(groupOwnerAddress!!)
                    }
                }
            }
        } else {
            println("----组尚未形成或已解散")
            groupOwnerAddress = null
        }
    }

    private suspend fun sendIdentityToGroupOwner(goIp: String) {
        val envelope = MessageEnvelope(
            id = randomUUID(),
            senderId = myId,
            senderName = Build.MODEL,
            payload = ChatPayload.Identity(myId), // 告诉群主我是谁
            timestamp = System.currentTimeMillis()
        )

        // 使用你现有的 transmit 函数，不需要传文件
        val success = transmit(goIp, envelope, null, null)
        if (success) {
            println("----WFD: 身份名片发送成功")
        } else {
            println("----WFD: 身份名片发送失败，准备重试...")
            delay(2000)
            sendIdentityToGroupOwner(goIp) // 简单重试一次
        }
    }

    // --- 2. 发现与连接逻辑 ---

    override fun startDiscovery() {
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { /* 扫描启动成功 */
                println("----扫描启动成功")
            }

            override fun onFailure(reason: Int) { /* 处理失败 */
                println("----扫描启动失败:$reason")
            }
        })
    }

    override fun stopDiscovery() {
        manager?.stopPeerDiscovery(channel, null)
        _peers.value = emptyList()
    }

    override suspend fun connect(peer: P2PPeer): Boolean = suspendCancellableCoroutine { cont ->
        val p2pPeer = peer as WifiDirectPeer
        val config = WifiP2pConfig().apply {
            deviceAddress = p2pPeer.mac
        }

        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true) { cause, _, _ ->
                    println("-----manager?.connect - onSuccess:$cause")
                }
            }

            override fun onFailure(reason: Int) {
                cont.resume(false) { cause, _, _ ->
                    println("-----manager?.connect - onFailure:$cause")
                }
            }
        })
    }

    override fun disconnect(peer: P2PPeer) {
        manager?.removeGroup(channel, null)
        stopMessageServer()
    }

    override suspend fun sendText(peer: P2PPeer, envelope: MessageEnvelope): Boolean {
        // 在 P2P 中，如果我是 GC，目标 IP 就是 GO 地址；
        // 如果我是 GO，目标 IP 需要通过之前的交互记录（Wi-Fi Direct 不直接暴露客户端 IP）
        // 简化逻辑：假设目前是 1对1 且发送给 Group Owner
//        val targetIp = getTargetIp(peer) ?: return false
        val targetIp = getTargetIp(peer) //?: return false
        println("------targetIP:$targetIp，isGo:${isGo}, groupOwnerAddress:${groupOwnerAddress}, peer:${peer}")
        if (targetIp == null) return false

        return transmit(targetIp, envelope, null, null)
    }

    override suspend fun sendMedia(
        peer: P2PPeer,
        envelope: MessageEnvelope,
        file: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean {
        val targetIp = getTargetIp(peer) ?: return false
        return transmit(targetIp, envelope, file, onProgress)
    }

    private suspend fun transmit(
        ip: String,
        envelope: MessageEnvelope,
        file: File?,
        onProgress: (suspend (Float) -> Unit)?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = aSocket(selectorManager).tcp().connect(ip, messagePort)
            val writeChannel = socket.openWriteChannel(autoFlush = false)

            // 1. Header
            val header =
                AppJson.instance.encodeToString(MessageEnvelope.serializer(), envelope) + "\n"


            println("-------发送-header:$header")

            writeChannel.writeStringUtf8(header)
            writeChannel.flush()

            // 2. File Body
            file?.let {
                it.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var totalSent = 0L
                    val length = it.length()
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        writeChannel.writeFully(buffer, 0, read)
                        totalSent += read
                        onProgress?.invoke(totalSent.toFloat() / length)
                        writeChannel.flush()
                    }
                }
            }

            writeChannel.flushAndClose()
            socket.close()


            println("----发送完成")

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- 4. 接收服务器 (TCP Server) ---

    override fun startMessageServer() {
        serverJob?.cancel()
        serverJob = scope.launch {
            val serverSocket =
                aSocket(selectorManager).tcp().bind(InetSocketAddress("0.0.0.0", messagePort)) {
                    reuseAddress = true
                }
            try {
                while (isActive) {
                    val client = serverSocket.accept()
                    launch { handleIncomingClient(client) }
                }
            } finally {
                serverSocket.close()
            }
        }
    }

    private suspend fun handleIncomingClient(socket: Socket) {
        socket.use { s ->
            // 关键：从 Socket 拿到连接方的 IP 地址
            val remoteIp = (s.remoteAddress as? InetSocketAddress)?.hostname ?: ""


            println("----remoteIp:$remoteIp")

            val readChannel = s.openReadChannel()
            try {
                val headerLine = readChannel.readUTF8Line() ?: return

                println("-----接收-headerLine:$headerLine")

                val envelope = AppJson.instance.decodeFromString<MessageEnvelope>(headerLine)

                when (envelope.payload) {
                    is ChatPayload.Identity -> {
                        // --- 核心逻辑：动态填充 IP ---
                        updatePeerIp(envelope.senderId, remoteIp)
                        println("----WFD: 握手成功！设备 ${envelope.senderId} 的 IP 是 $remoteIp")
                        return@use // 握手包处理完直接关闭
                    }

                    is ChatPayload.Media -> {
                        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
                        val file = File(mediaDir, "p2p_${envelope.id}_${envelope.payload.fileName}")
                        file.outputStream().use { output ->
                            readChannel.copyTo(output)
                        }
                        val updatedPayload = envelope.payload.copy(localPath = file.absolutePath)
//                        _messageFlow.emit(envelope.copy(payload = updatedPayload))
                    }

                    else -> {
//                        _messageFlow.emit(envelope)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 更新列表函数
    private fun updatePeerIp(deviceId: String, ip: String) {
        val current = _peers.value.toMutableList()
        val index = current.indexOfFirst { it.id == deviceId }
        if (index != -1) {
            val old = current[index] as WifiDirectPeer
            current[index] = old.copy(ip = ip) // 填充 IP
            _peers.value = current
        }
    }

    override fun stopMessageServer() {
        serverJob?.cancel()
    }

    override suspend fun sendPayload(
        targetIp: String,
        payload: ChatPayload
    ): Boolean = false

    private fun getTargetIp(peer: P2PPeer): String? {
        // 在 Wi-Fi Direct 中，GC 永远知道 GO 的地址（通常是 192.168.49.1）
        // 如果我是 GC，我直接发给 groupOwnerAddress
        return if (!isGo) groupOwnerAddress else (peer as? WifiDirectPeer)?.ip
    }
}