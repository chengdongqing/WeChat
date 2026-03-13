package top.chengdongqing.wechat.data.network.service.modules

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.connection.wifi.ConnectionManager
import top.chengdongqing.wechat.data.network.connection.wifi.SocketClient
import top.chengdongqing.wechat.data.network.connection.wifi.SocketServer
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiDirectChatModule @Inject constructor(
    private val socketServer: SocketServer,
    private val socketClient: SocketClient,
    private val connectionManager: ConnectionManager,
    private val profileRepository: ProfileRepository,
    private val activeSessionManager: ActiveSessionManager,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "WiFiDirectChatModule"
        private const val DEFAULT_PORT = 8888
    }

    private val p2pManager by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }
    private val channel by lazy {
        p2pManager.initialize(context, Looper.getMainLooper(), null)
    }
    private var receiver: BroadcastReceiver? = null
    private var isGroupOwner = false

    suspend fun prepare() {
        removeGroup()
        socketServer.start(DEFAULT_PORT)
        registerReceiver()
        Log.d(TAG, "WiFi Direct 模块已就绪，等待用户选择角色")
    }

    // 用户点「创建群组」
    suspend fun startAsOwner() {
        createGroup()
        isGroupOwner = true
        Log.d(TAG, "已创建 P2P 组，等待 Client 加入")
    }

    // 用户点「加入群组」，只需要扫描，连接在用户选设备时触发
    fun startAsClient() {
        isGroupOwner = false
        scope.launch {
            removeGroup()  // 先清掉旧组
            Log.d(TAG, "Client 模式，开始扫描")
        }
    }

    fun stop() {
        p2pManager.removeGroup(channel, null)
        socketServer.stop()
        connectionManager.closeAll()
        unregisterReceiver()
        Log.d(TAG, "WiFi Direct 聊天模块已停止")
    }

    @SuppressLint("MissingPermission")
    private suspend fun createGroup() = suspendCancellableCoroutine { cont ->
        p2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "P2P 组创建成功，我是 GO")
                if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "P2P 组创建失败: $reason，走普通连接流程")
                if (cont.isActive) cont.resume(Unit) { _, _, _ -> } // 失败不阻塞，继续启动
            }
        })
    }

    private suspend fun removeGroup() = suspendCancellableCoroutine { cont ->
        p2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
            }

            override fun onFailure(reason: Int) {
                if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
            }
        })
    }

    private fun registerReceiver() {
        Log.d(TAG, "注册 P2P 广播")
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "收到广播: ${intent.action}")
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        p2pManager.requestConnectionInfo(channel) { info ->
                            if (info != null && info.groupFormed) {
                                val goIp = info.groupOwnerAddress?.hostAddress
                                val goUserId = activeSessionManager.activeSessionId
                                    ?: return@requestConnectionInfo

                                Log.d(
                                    TAG,
                                    "P2P 组已建立 - isGroupOwner: ${info.isGroupOwner}, goIp: $goIp"
                                )
                                if (info.isGroupOwner) {
                                    Log.d(TAG, "我是 GO，等待 Client 连接")
                                } else if (goIp != null) {
                                    scope.launch {
                                        connectAsClient(goUserId, goIp)
                                    }
                                }
                            } else {
                                Log.d(TAG, "P2P 组已解散")
                                connectionManager.closeAll()
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
    }

    private suspend fun connectAsClient(goUserId: String, goIp: String) =
        withContext(Dispatchers.IO) {
            val myUserId = profileRepository.getProfile()?.id ?: return@withContext

            socketClient.connect(
                userId = goUserId,
                host = goIp,
                port = DEFAULT_PORT,
                myUserId = myUserId
            )

            Log.d(TAG, "connectAsClient：已连接")
        }

    private fun unregisterReceiver() {
        receiver?.let {
            runCatching { context.unregisterReceiver(it) }
            receiver = null
        }
    }
}