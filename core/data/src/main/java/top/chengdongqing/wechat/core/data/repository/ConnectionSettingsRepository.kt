package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.data.model.ConnectionMode

interface ConnectionSettingsRepository {
    val connectionMode: Flow<ConnectionMode>
    suspend fun setConnectionMode(mode: ConnectionMode)
}
