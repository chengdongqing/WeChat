package top.chengdongqing.wechat.data.network.connection

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketReader
import top.chengdongqing.wechat.data.network.model.PacketWriter
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 连接实例
 *
 * @param transferMutex      文件传输时加锁，保证 META+CHUNK 序列原子不被插入
 * @param activeTransferCount 传输引用计数，> 0 时心跳自动暂停
 * @param lastPongTime       最后收到 Pong 的时间戳，超时判断依据
 */
data class PeerConnection(
    val userId: String,
    val reader: PacketReader,
    val writer: PacketWriter,
    val receiveChannel: Channel<Packet> = Channel(Channel.UNLIMITED),
    val transferMutex: Mutex = Mutex(),
    val activeTransferCount: AtomicInteger = AtomicInteger(0),
    val lastPongTime: AtomicLong = AtomicLong(System.currentTimeMillis()),
    var heartbeatJob: Job? = null,
    private val isActiveProvider: () -> Boolean,
    private val closeAction: () -> Unit
) {
    val isActive: Boolean get() = isActiveProvider()

    fun incrementTransferCount() = activeTransferCount.incrementAndGet()

    fun decrementTransferCount() {
        if (activeTransferCount.decrementAndGet() <= 0) {
            lastPongTime.set(System.currentTimeMillis())
        }
    }

    fun close() {
        runCatching {
            heartbeatJob?.cancel()
            receiveChannel.close()
            reader.close()
            writer.close()
            closeAction()
        }
    }
}