package top.chengdongqing.wechat.core.network.transfer

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 传输状态
 */
enum class TransferState {
    Active,      // 传输中
    Paused,      // 已暂停
    Cancelled    // 已取消
}

/**
 * 文件传输状态管理器
 */
@Singleton
class TransferManager @Inject constructor() {

    private val states = ConcurrentHashMap<String, TransferState>()
    private val pauseLocks = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /**
     * 标记为已取消
     */
    fun setCancelled(messageId: String) {
        states[messageId] = TransferState.Cancelled
        pauseLocks.remove(messageId)?.cancel()
    }

    /**
     * 标记为已暂停
     */
    fun setPaused(messageId: String) {
        states[messageId] = TransferState.Paused
        pauseLocks.putIfAbsent(messageId, CompletableDeferred())
    }

    /**
     * 标记为已恢复
     */
    fun setResumed(messageId: String) {
        states[messageId] = TransferState.Active
        pauseLocks.remove(messageId)?.complete(Unit)
    }

    /**
     * 是否已取消
     */
    fun isCancelled(messageId: String): Boolean =
        states[messageId] == TransferState.Cancelled

    /**
     * 是否有活跃的传输协程
     */
    fun hasActiveTransfer(messageId: String): Boolean = states.containsKey(messageId)

    /**
     * 如果当前处于暂停状态
     */
    suspend fun awaitIfPaused(messageId: String) {
        pauseLocks[messageId]?.await()
    }

    /**
     * 传输完成或失败后清理
     */
    fun remove(messageId: String) {
        states.remove(messageId)
        pauseLocks.remove(messageId)?.cancel()
    }
}