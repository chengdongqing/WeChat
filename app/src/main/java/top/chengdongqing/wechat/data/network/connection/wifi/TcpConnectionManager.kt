package top.chengdongqing.wechat.data.network.connection.wifi

import kotlinx.coroutines.CoroutineScope
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 连接管理器
 */
@Singleton
class TcpConnectionManager @Inject constructor(
    override val e2e: E2ESessionManager,
    override val connectionInfoDao: ConnectionInfoDao,
    @param:IoScope override val scope: CoroutineScope
) : ConnectionManager(e2e, connectionInfoDao, scope) {
    override val tag = "TcpConnectionManager"
}