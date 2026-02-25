package top.chengdongqing.wechat.data.network.connection

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import java.net.Socket
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
    val socket: Socket,
    val reader: PacketReader,
    val writer: PacketWriter,
    val receiveChannel: Channel<Packet> = Channel(Channel.UNLIMITED),
    val transferMutex: Mutex = Mutex(),
    val activeTransferCount: AtomicInteger = AtomicInteger(0),
    val lastPongTime: AtomicLong = AtomicLong(System.currentTimeMillis()),
    var heartbeatJob: Job? = null
) {
    val isActive: Boolean get() = socket.isConnected && !socket.isClosed

    /** 传输开始，心跳自动暂停 */
    fun incrementTransferCount() = activeTransferCount.incrementAndGet()

    /**
     * 传输结束
     *
     * 计数归零时重置 [lastPongTime]，避免传输期间心跳暂停积累的时间触发误判超时。
     */
    fun decrementTransferCount() {
        if (activeTransferCount.decrementAndGet() <= 0) {
            lastPongTime.set(System.currentTimeMillis())
        }
    }

    /** 关闭连接，取消心跳并释放所有 IO 资源 */
    fun close() {
        runCatching {
            heartbeatJob?.cancel()
            receiveChannel.close()
            reader.close()
            writer.close()
            socket.close()
        }
    }
}