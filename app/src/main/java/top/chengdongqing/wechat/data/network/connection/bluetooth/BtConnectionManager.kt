package top.chengdongqing.wechat.data.network.connection.bluetooth

import kotlinx.coroutines.CoroutineScope
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.network.connection.AbstractConnectionManager
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 蓝牙连接管理器
 */
@Singleton
class BtConnectionManager @Inject constructor(
    override val e2e: E2ESessionManager,
    override val connectionInfoDao: ConnectionInfoDao,
    override val profileRepository: ProfileRepository,
    override val contactRepository: ContactRepository,
    @param:IoScope override val scope: CoroutineScope
) : AbstractConnectionManager(e2e, connectionInfoDao, profileRepository, contactRepository, scope) {
    override val tag = "BtConnectionManager"
}