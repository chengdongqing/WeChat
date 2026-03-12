package top.chengdongqing.wechat.features.settings.domain.usecase

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.util.clearAllCache
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.DisplaySettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.NotificationSettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.PrivacySettingsRepository

class LogoutUseCase @Inject constructor(
    private val database: WeDatabase,
    private val profileRepository: ProfileRepository,
    private val displaySettingsRepository: DisplaySettingsRepository,
    private val chatSettingsRepository: ChatSettingsRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val privacySettingsRepository: PrivacySettingsRepository,
    private val connectionManager: ConnectionManager,
    private val e2eSessionManager: E2ESessionManager,
    private val privateFileManager: PrivateFileManager,
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
            clearDataStore()

            // 删除所有媒体文件
            privateFileManager.clearAll()

            // 清除所有缓存
            context.clearAllCache()
        }
    }.onFailure {
        Log.e(TAG, "退出登录失败", it)
    }

    private suspend fun clearDataStore() {
        profileRepository.clear()
        displaySettingsRepository.clearAll()
        chatSettingsRepository.clearAll()
        notificationSettingsRepository.clearAll()
        privacySettingsRepository.clearAll()
    }
}