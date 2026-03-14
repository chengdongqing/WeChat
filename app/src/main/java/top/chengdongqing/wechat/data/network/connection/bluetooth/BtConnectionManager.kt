package top.chengdongqing.wechat.data.network.connection.bluetooth

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtConnectionManager.Companion.IDLE_TIMEOUT_MS
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 蓝牙连接管理器
 *
 * - 维护当前所有活跃的对等连接
 * - 发送数据包（普通 / 原子传输）
 * - 驱动心跳检测和空闲超时
 * - 广播连接状态变化事件
 */
@Singleton
class BtConnectionManager @Inject constructor(
    override val e2e: E2ESessionManager,
    override val connectionInfoDao: ConnectionInfoDao,
    @param:IoScope override val scope: CoroutineScope
) : ConnectionManager(e2e, connectionInfoDao, scope) {
    companion object {
        // 超过此时长无任何收发则主动断开，蓝牙同时连接数有限，尽早释放
        private const val IDLE_TIMEOUT_MS = 5 * 60 * 1000L
    }

    override val tag = "BtConnectionManager"

    override suspend fun register(conn: PeerConnection) {
        super.register(conn)

        startIdleTimer(conn)
    }

    /**
     * 空闲超时检测：每隔 [IDLE_TIMEOUT_MS] 检查一次上次通信时间，
     * 超时则主动断开。用 lastPongTime 作为"最后活跃时间"，
     * 普通发包和收到 Pong 都会刷新它，因此心跳本身也能阻止误触发。
     */
    private fun startIdleTimer(conn: PeerConnection) {
        scope.launch {
            while (conn.isActive) {
                delay(IDLE_TIMEOUT_MS)
                val idle = System.currentTimeMillis() - conn.lastPongTime.get()
                if (idle >= IDLE_TIMEOUT_MS) {
                    Log.d(tag, "连接空闲超时，断开: ${conn.userId}")
                    disconnect(conn.userId)
                    break
                }
            }
        }
    }
}