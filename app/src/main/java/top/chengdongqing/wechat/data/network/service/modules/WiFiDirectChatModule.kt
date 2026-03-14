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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.connection.wifi.TcpConnectionManager
import top.chengdongqing.wechat.data.network.connection.wifi.TcpSocketClient
import top.chengdongqing.wechat.data.network.connection.wifi.TcpSocketServer
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiDirectChatModule @Inject constructor(
    private val socketServer: TcpSocketServer,
    private val socketClient: TcpSocketClient,
    private val connectionManager: TcpConnectionManager,
    private val profileRepository: ProfileRepository,
    private val activeSessionManager: ActiveSessionManager,
    private val messageReceiver: MessageReceiver,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope,
) {
    private companion object {
        private const val TAG = "WiFiDirectChatModule"
        private const val PORT = 8888
    }

    private val p2pManager by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }
    private val channel by lazy {
        p2pManager.initialize(context, Looper.getMainLooper(), null)
    }
    private var connectionReceiver: BroadcastReceiver? = null

    /**
     * 初始化模块：清理旧组、申请锁、启动服务、监听连接状态
     */
    suspend fun prepare() {
        removeGroup()
        socketServer.start(PORT)
        messageReceiver.start()
        observeConnectionState()

        Log.d(TAG, "WiFi Direct 模块已就绪，等待用户选择角色")
    }

    suspend fun startAsOwner() = createGroup()

    suspend fun startAsClient() = removeGroup()

    /**
     * 停止模块，释放所有资源
     */
    suspend fun stop() {
        removeGroup()
        unregisterConnectionReceiver()
        connectionManager.closeAll()
        socketServer.stop()

        Log.d(TAG, "WiFi Direct 聊天模块已停止")
    }

    @SuppressLint("MissingPermission")
    private suspend fun createGroup() = suspendCancellableCoroutine { cont ->
        p2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resumeIfActive(Unit)
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "P2P 组创建失败: $reason，继续执行")
                cont.resumeIfActive(Unit)
            }
        })
    }

    private suspend fun removeGroup() = suspendCancellableCoroutine { cont ->
        p2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = cont.resumeIfActive(Unit)
            override fun onFailure(reason: Int) = cont.resumeIfActive(Unit)
        })
    }

    /**
     * 监听 P2P 连接状态变化，群组建立后由客户端主动发起 TCP 连接
     */
    private fun observeConnectionState() {
        connectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) return

                p2pManager.requestConnectionInfo(channel) { info ->
                    if (info != null && info.groupFormed) {
                        val goIp =
                            info.groupOwnerAddress?.hostAddress ?: return@requestConnectionInfo
                        val goUserId =
                            activeSessionManager.activeSessionId ?: return@requestConnectionInfo

                        if (!info.isGroupOwner) {
                            scope.launch {
                                delay(1000)
                                connectAsClient(goUserId, goIp)
                            }
                        }
                    } else {
                        connectionManager.closeAll()
                    }
                }
            }
        }

        context.registerReceiver(
            connectionReceiver,
            IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION),
        )
    }

    private suspend fun connectAsClient(goUserId: String, goIp: String) {
        val myUserId = profileRepository.getProfile()?.id ?: return

        socketClient.connect(
            userId = goUserId,
            host = goIp,
            port = PORT,
            myUserId = myUserId
        )
    }

    private fun unregisterConnectionReceiver() {
        connectionReceiver?.let {
            runCatching { context.unregisterReceiver(it) }
            connectionReceiver = null
        }
    }
}

private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) resume(value) { _, _, _ -> }
}