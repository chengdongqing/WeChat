package top.chengdongqing.wechat.data.network.transfer

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 当前文件传输管理
 * 方便停止传输
 */
@Singleton
class TransferManager @Inject constructor() {

    private val cancelledTransfers = ConcurrentHashMap<String, Boolean>()

    /**
     * 标记传输状态
     */
    fun setCancelled(messageId: String) {
        cancelledTransfers[messageId] = true
    }

    /**
     * 检查是否已取消
     */
    fun isCancelled(messageId: String): Boolean {
        return cancelledTransfers.containsKey(messageId)
    }

    /**
     * 传输完成或失败后及时清理，避免内存泄漏
     */
    fun remove(messageId: String) {
        cancelledTransfers.remove(messageId)
    }
}