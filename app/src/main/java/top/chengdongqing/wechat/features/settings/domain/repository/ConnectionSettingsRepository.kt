package top.chengdongqing.wechat.features.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.network.connection.ConnectionMode

interface ConnectionSettingsRepository {

    /**
     * 连接模式
     */
    val connectionMode: Flow<ConnectionMode>

    suspend fun setConnectionMode(mode: ConnectionMode)

    /**
     * 退出登录时清除所有设置
     */
    suspend fun clearAll()
}