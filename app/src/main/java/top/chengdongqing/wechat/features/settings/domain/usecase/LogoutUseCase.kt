package top.chengdongqing.wechat.features.settings.domain.usecase

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.util.clearAllCaches
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.network.connection.wifi.TcpConnectionManager
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.security.LocalIdentity

class LogoutUseCase @Inject constructor(
    private val database: WeDatabase,
    private val dataStoreManager: DataStoreManager,
    private val connectionManager: TcpConnectionManager,
    private val e2eSessionManager: E2ESessionManager,
    private val privateFileManager: PrivateFileManager,
    private val localIdentity: LocalIdentity,
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LogoutUseCase"
    }

    suspend operator fun invoke(): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            // 断开连接，停止一切网络活动
            connectionManager.closeAll()

            // 清除内存中的 E2E session
            e2eSessionManager.clearAll()

            // 清空数据库
            database.clearAllTables()

            // 清空 DataStore
            dataStoreManager.clearAll()

            // 删除所有媒体文件
            privateFileManager.clearAll()

            // 清除所有缓存
            context.clearAllCaches()

            // 删除密钥
            localIdentity.clearIdentity()
        }
    }.onFailure {
        Log.e(TAG, "退出登录失败", it)
    }
}

@Singleton
class DataStoreManager @Inject constructor(
    private val allStores: Set<@JvmSuppressWildcards DataStore<Preferences>>,
) {

    /**
     * 清理所有已注册的 DataStore 数据
     * 使用 supervisorScope 确保即便其中一个清理失败，其他也能继续
     */
    suspend fun clearAll() {
        supervisorScope {
            allStores.forEach { store ->
                launch {
                    runCatching {
                        store.edit { it.clear() }
                    }
                }
            }
        }
    }
}