package top.chengdongqing.wechat.core.network.crypto

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketWriter

/**
 * 加密包写入器
 *
 * [write] 和 [writeNoFlush] 均在持锁状态下执行底层写操作，
 * 保证并发协程的字节不会在 Socket 缓冲区中交织。
 *
 * 锁的粒度是"单次 Packet 写入"，不覆盖整个文件传输过程，
 * 因此多个文件传输的分片可以并发地交替写入同一个 Socket，
 * 接收方通过 FILE_CHUNK body 中的 messageId 前缀区分归属。
 */
class EncryptingPacketWriter(
    private val writer: PacketWriter,
    private val userId: String,
    private val e2e: E2ESessionManager,
    private val writeMutex: Mutex
) {
    /**
     * 加密后写入并立即 flush（用于控制包、小文件 META）
     */
    suspend fun write(packet: Packet) {
        val encrypted = e2e.encryptPacket(userId, packet)
        writeMutex.withLock {
            writer.write(encrypted)
        }
    }

    /**
     * 加密后写入但不 flush（用于大文件分片，由调用方在传输结束时统一 flush）
     *
     * 虽然不 flush，但 writeMutex 依然保证写入本身的原子性，
     * 不同传输协程的字节不会交织。
     */
    suspend fun writeNoFlush(packet: Packet) {
        val encrypted = e2e.encryptPacket(userId, packet)
        writeMutex.withLock {
            writer.writeNoFlush(encrypted)
        }
    }
}