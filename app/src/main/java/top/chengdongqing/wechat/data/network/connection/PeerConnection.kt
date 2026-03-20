package top.chengdongqing.wechat.data.network.connection

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketReader
import top.chengdongqing.wechat.data.network.model.PacketWriter
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 连接实例：维护与特定用户之间的底层通信会话
 */
data class PeerConnection(
    /**
     * 远端用户的唯一标识
     */
    val userId: String,

    /**
     * 封装的 Socket 读取器，用于解析传入的二进制包
     */
    val reader: PacketReader,

    /**
     * 封装的 Socket 写入器，用于发送二进制包
     */
    val writer: PacketWriter,

    /**
     * 接收到的所有非文件分片数据包（如文本、回执）会放入此管道
     */
    val receiveChannel: Channel<Packet> = Channel(Channel.UNLIMITED),

    /**
     * 写入互斥锁：确保同一时间只有一个协程在向 Socket 缓冲区写数据，防止字节流在物理层交织错乱
     */
    val writeMutex: Mutex = Mutex(),

    /**
     * 并发传输文件的数量
     */
    val maxConcurrentTransfers: Semaphore = Semaphore(TransferConfig.CONCURRENT_TRANSFERS),

    /**
     * 线程安全地记录当前正在进行的任务数，用于判断连接是否处于闲置状态
     */
    val activeTransferCount: AtomicInteger = AtomicInteger(0),

    /**
     * 记录最后一次收到心跳响应的时间戳，用于连接存活检测（Keep-Alive）
     */
    val lastPongTime: AtomicLong = AtomicLong(System.currentTimeMillis()),

    /**
     * 心跳定时任务的句柄，用于连接断开时取消任务
     */
    var heartbeatJob: Job? = null,

    /**
     * 外部传入的状态检查函数（对于Wi-Fi和蓝牙可以有自己的判断方式）
     */
    private val isActiveProvider: () -> Boolean,

    /**
     * 外部传入的连接关闭回调
     */
    private val closeAction: () -> Unit
) {
    val isActive: Boolean get() = isActiveProvider()

    /**
     * 增加活跃任务数
     */
    fun incrementTransferCount() = activeTransferCount.incrementAndGet()

    /**
     * 减少活跃任务数，当任务归零时，更新最后活跃时间，以便后续可能的超时回收
     */
    fun decrementTransferCount() {
        if (activeTransferCount.decrementAndGet() <= 0) {
            lastPongTime.set(System.currentTimeMillis())
        }
    }

    /**
     * 关闭连接并释放所有底层资源
     */
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