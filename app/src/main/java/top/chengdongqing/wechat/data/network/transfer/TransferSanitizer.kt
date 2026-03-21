package top.chengdongqing.wechat.data.network.transfer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.network.messaging.ChunkStorageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferSanitizer @Inject constructor(
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val chunkStorageManager: ChunkStorageManager,
    @param:IoScope private val scope: CoroutineScope
) {

    fun sanitize() {
        scope.launch {
            launch { markSendingAsPaused() }
            launch { cleanupStaleChunks() }
        }
    }

    /**
     * 改为 Paused 让 UI 显示暂停状态，用户可以点继续触发重新发送
     */
    private suspend fun markSendingAsPaused() {
        // 将所有发送中的消息设为暂停
        messageDao.pauseOngoingTransfers()
        // 将所有完全没有发送出去的消息设为失败
        messageDao.failUnstartedMessages()
        // 将所有发送中的会话设为默认
        chatSessionDao.resetAllSendingSessions()
    }

    /**
     * 清理超过 24 小时的过期分片
     */
    private suspend fun cleanupStaleChunks() {
        chunkStorageManager.cleanupStaleTransfers()
    }
}