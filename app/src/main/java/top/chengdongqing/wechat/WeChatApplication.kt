package top.chengdongqing.wechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.common.util.clearAllCaches
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.network.transfer.TransferSanitizer

@HiltAndroidApp
class WeChatApplication : Application() {

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var transferSanitizer: TransferSanitizer

    @Inject
    lateinit var assetReferenceManager: AssetReferenceManager

    override fun onCreate() {
        super.onCreate()

        /**
         * 自动清理之前产生的缓存
         */
        scope.launch {
            clearAllCaches()
            assetReferenceManager.cleanupOrphans()
        }

        /**
         * 将所有发送中的消息状态更新为暂停，方便手动恢复发送
         * 清理过期的分片文件等
         */
        transferSanitizer.sanitize()
    }
}
