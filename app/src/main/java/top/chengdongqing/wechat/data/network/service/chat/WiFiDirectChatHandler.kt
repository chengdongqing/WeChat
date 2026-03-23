package top.chengdongqing.wechat.data.network.service.chat

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
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.connection.wifi.TcpSocketClient
import top.chengdongqing.wechat.data.network.connection.wifi.TcpSocketServer
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.service.ServiceModule
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wi-Fi直连聊天模块
 */
@Singleton
class WiFiDirectChatHandler @Inject constructor(
    private val socketServer: TcpSocketServer,
    private val socketClient: TcpSocketClient,
    private val connectionManager: ConnectionManager,
    private val activeSessionManager: ActiveSessionManager,
    private val messageReceiver: MessageReceiver,
    private val profileRepository: ProfileRepository,
    private val connectionInfoDao: ConnectionInfoDao,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope
) : ServiceModule {
    private companion object {
        private const val TAG = "WiFiDirectChatHandler"
        private const val PORT = 8888
    }

    private val p2pManager by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }
    private val channel by lazy {
        p2pManager.initialize(context, Looper.getMainLooper(), null)
    }
    private var connectionReceiver: BroadcastReceiver? = null

    private val myUserId: String
        get() = profileRepository.requireUserId()

    override fun start() {
        runCatching {
            scope.launch {
                removeGroup()
                socketServer.start(PORT)
                messageReceiver.start()
            }
            observeConnectionState()
        }.onSuccess {
            Log.d(TAG, "WiFi Direct 聊天模块已启动")
        }.onFailure {
            Log.e(TAG, "WiFi Direct 聊天模块启动失败", it)
        }
    }

    /**
     * 停止模块，释放所有资源
     */
    override fun stop() {
        runCatching {
            scope.launch { removeGroup() }
            unregisterConnectionReceiver()
            connectionManager.closeAll()
            socketServer.stop()
        }.onSuccess {
            Log.d(TAG, "WiFi Direct 聊天模块已停止")
        }
    }

    suspend fun startAsOwner() = createGroup()

    suspend fun startAsClient() = removeGroup()

    /**
     * 创建组
     */
    @SuppressLint("MissingPermission")
    private suspend fun createGroup() {
        removeGroup()
        suspendCancellableCoroutine { cont ->
            p2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    cont.resumeIfActive(Unit)
                }

                override fun onFailure(reason: Int) {
                    Log.w(TAG, "P2P 组创建失败: $reason")
                    cont.resumeIfActive(Unit)
                }
            })
        }
    }

    /**
     * 解散组
     */
    private suspend fun removeGroup() = suspendCancellableCoroutine { cont ->
        p2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = cont.resumeIfActive(Unit)
            override fun onFailure(reason: Int) = cont.resumeIfActive(Unit)
        })
    }

    /**
     * 监听连接状态变化，群组建立后由客户端主动发起 TCP 连接
     */
    private fun observeConnectionState() {
        connectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) return

                p2pManager.requestConnectionInfo(channel) { info ->
                    if (info != null && info.groupFormed) {
                        val goIp = info.groupOwnerAddress?.hostAddress
                        val goUserId = activeSessionManager.activeSessionId

                        if (!info.isGroupOwner && goIp != null && goUserId != null) {
                            scope.launch {
                                delay(1000)

                                // 连接Go
                                connectToGroupOwner(goUserId, goIp).onSuccess {
                                    // 保存连接信息（主要为保存已连接的状态）
                                    saveToDB(goUserId, goIp)
                                }
                            }
                        }
                    } else {
                        // 解散组后关闭所有连接
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

    /**
     * 主动连接 Group Owner
     */
    private suspend fun connectToGroupOwner(goUserId: String, goIp: String, goPort: Int = PORT) =
        socketClient.connect(
            userId = goUserId,
            host = goIp,
            port = goPort,
            myUserId = myUserId
        )

    /**
     * 保存连接信息到数据库
     */
    suspend fun saveToDB(userId: String, goIp: String, goPort: Int = PORT) {
        connectionInfoDao.upsert(
            ConnectionInfoEntity(
                userId = userId,
                lanIpAddress = goIp,
                lanPort = goPort,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
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