package top.chengdongqing.wechat.data.network.config

/**
 * 传输性能调参数配置
 */
object TransferConfig {

    /**
     * Socket 发送缓冲区: 512KB
     *
     * 默认 Android 约 64-128KB，对于 LAN 大文件传输太小。
     * 512KB 可以让内核攒足够的数据后一次性推送到网卡，
     * 减少 syscall 次数并充分利用 TCP 窗口。
     */
    const val SOCKET_SEND_BUFFER = 512 * 1024       // 512KB

    /**
     * Socket 接收缓冲区: 512KB
     *
     * 与发送端对称，确保接收窗口不成为瓶颈。
     */
    const val SOCKET_RECV_BUFFER = 512 * 1024       // 512KB

    /**
     * BufferedOutputStream 缓冲区: 256KB
     *
     * DataOutputStream → BufferedOutputStream → Socket OutputStream
     * 缓冲层把多次小写入合并成一次 write syscall。
     * 256KB 在 flush 时刚好触发 1-2 次内核拷贝，开销最小。
     */
    const val STREAM_WRITE_BUFFER = 256 * 1024      // 256KB

    /**
     * BufferedInputStream 缓冲区: 256KB
     */
    const val STREAM_READ_BUFFER = 256 * 1024       // 256KB

    /**
     * 文件分片大小
     */
    const val FILE_CHUNK_SIZE = 256 * 1024          // 256KB
    const val FILE_CHUNK_SIZE_BT = 64 * 1024        // 64KB

    /**
     * 分片传输阈值（字节），大于此值才走协商+分片流程，否则直传
     */
    const val CHUNK_TRANSFER_THRESHOLD = 4 * 1024 * 1024  // 4MB

    /**
     * 单个 Packet 最大长度
     *
     * FILE_CHUNK 最大 256KB，加上其他消息类型（JSON）一般 < 10KB，
     * 1MB 留足余量。超过此值视为异常数据，拒绝接收防止内存攻击。
     */
    const val MAX_PACKET_LENGTH = 1 * 1024 * 1024   // 1MB

    /**
     * TCP 连接超时
     */
    const val CONNECT_TIMEOUT = 10_000

    /**
     * 握手阶段读超时
     */
    const val HANDSHAKE_TIMEOUT = 10_000

    /**
     * 心跳
     */
    const val PING_INTERVAL = 15_000L               // Ping 发送间隔
    const val PONG_TIMEOUT = 20_000L                // Pong 超时阈值

    /**
     * 进度回调间隔阈值
     */
    const val PROGRESS_REPORT_INTERVAL = 1024 * 1024L   // 1MB
    const val PROGRESS_REPORT_INTERVAL_BT = 1024 * 64L  // 64KB (蓝牙模式下)
}