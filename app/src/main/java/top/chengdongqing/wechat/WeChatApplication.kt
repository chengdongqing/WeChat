package top.chengdongqing.wechat

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.file.cache.clearAllCaches
import top.chengdongqing.wechat.core.network.transfer.TransferSanitizer
import top.chengdongqing.wechat.core.runtime.IoScope

@HiltAndroidApp
class WeChatApplication : Application() {

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var transferSanitizer: TransferSanitizer

    @Inject
    lateinit var assetReferenceManager: AssetReferenceManager

    @Inject
    lateinit var chatSessionRepository: ChatSessionRepository

    override fun onCreate() {
        super.onCreate()

        /**
         * 自动清理之前产生的缓存
         */
        scope.launch {
            clearAllCaches()
            assetReferenceManager.cleanupOrphans()
        }

        scope.launch {
            while (true) {
                runCatching {
                    chatSessionRepository.cleanupExpiredTemporarySessions()
                        .forEach { sessionId ->
                            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                                .cancel(sessionId.hashCode())
                        }
                }
                delay(TEMPORARY_CHAT_CLEANUP_INTERVAL_MS)
            }
        }

        /**
         * 将所有发送中的消息状态更新为暂停，方便手动恢复发送
         * 清理过期的分片文件等
         */
        transferSanitizer.sanitize()
    }

    private companion object {
        const val TEMPORARY_CHAT_CLEANUP_INTERVAL_MS = 60_000L
    }
}
