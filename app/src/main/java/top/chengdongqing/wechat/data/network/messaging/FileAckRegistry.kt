package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import top.chengdongqing.wechat.data.network.model.FileMetaAck
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件元数据 ACK 等待注册表
 *
 * 发送方发出 FILE_META 后，通过 [awaitAck] 挂起等待接收方回复；
 * 本机 MessageReceiver 收到 FILE_META_ACK 包后，调用 [complete] 唤醒等待协程。
 *
 * 内部使用 CompletableDeferred 实现挂起/恢复桥接。
 */
@Singleton
class FileAckRegistry @Inject constructor() {

    private companion object {
        const val TAG = "FileAckRegistry"
    }

    private val pending = ConcurrentHashMap<String, CompletableDeferred<FileMetaAck>>()

    /**
     * 挂起等待指定 messageId 的 ACK 回复
     *
     * @param messageId 消息 ID
     * @param timeoutMs 超时时间（毫秒），超时抛出 TimeoutCancellationException
     */
    suspend fun awaitAck(messageId: String, timeoutMs: Long): FileMetaAck {
        val deferred = CompletableDeferred<FileMetaAck>()
        pending[messageId] = deferred

        return try {
            withTimeout(timeoutMs) {
                deferred.await()
            }
        } finally {
            pending.remove(messageId)
        }
    }

    /**
     * 完成指定 messageId 的等待，唤醒发送方协程
     *
     * 由 MessageReceiver 在收到 FILE_META_ACK 包时调用
     */
    fun complete(messageId: String, ack: FileMetaAck) {
        val deferred = pending[messageId]
        if (deferred != null) {
            deferred.complete(ack)
        } else {
            Log.w(TAG, "收到 ACK 但无等待者: $messageId")
        }
    }

    /**
     * 取消指定 messageId 的等待（连接断开等异常场景）
     */
    fun cancel(messageId: String) {
        pending.remove(messageId)?.cancel()
    }

    /**
     * 清理所有等待
     */
    fun cancelAll() {
        pending.forEach { (_, deferred) -> deferred.cancel() }
        pending.clear()
    }
}