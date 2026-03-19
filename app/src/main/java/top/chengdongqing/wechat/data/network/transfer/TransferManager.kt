package top.chengdongqing.wechat.data.network.transfer

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
     *
     * 如果当前正暂停，取消暂停锁使挂起协程立即恢复
     */
    fun setCancelled(messageId: String) {
        states[messageId] = TransferState.Cancelled
        pauseLocks.remove(messageId)?.cancel()
    }

    /**
     * 标记为已暂停
     *
     * 创建一个 CompletableDeferred, 发送循环调用 awaitIfPaused 时会挂起
     */
    fun setPaused(messageId: String) {
        states[messageId] = TransferState.Paused
        pauseLocks.putIfAbsent(messageId, CompletableDeferred())
    }

    /**
     * 标记为已恢复
     *
     * complete 暂停锁，唤醒挂起的发送协程
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
     * 是否有活跃的传输协程（内存中有状态记录）
     *
     * 用于判断 resumeTransfer 时是否需要重新启动发送：
     * - true：有协程在跑（或暂停中），setResumed 可以唤醒它
     * - false：没有协程（app 重启后），需要重新启动发送流程
     */
    fun hasActiveTransfer(messageId: String): Boolean =
        states.containsKey(messageId)

    /**
     * 如果当前处于暂停状态，挂起直到恢复或取消
     *
     * 发送循环每发完一个 chunk 后调用此方法
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