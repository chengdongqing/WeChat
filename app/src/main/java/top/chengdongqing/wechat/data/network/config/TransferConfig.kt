package top.chengdongqing.wechat.data.network.config

/**
 * 传输性能调优参数
 *
 * WiFi LAN 典型带宽 50-800 Mbps，延迟 < 5ms。
 * 瓶颈通常不在网络本身，而在:
 * 1. 用户态 ↔ 内核态拷贝次数（buffer 太小 → syscall 过多）
 * 2. 磁盘 I/O（读写跟不上网卡速度）
 * 3. Nagle + Delayed ACK 交互导致的 40ms 惩罚
 * 4. 每个 chunk 的 Packet 头开销（5 bytes per chunk）
 *
 * 以下参数针对 LAN 场景调优，目标: 跑满百兆网卡 (~12MB/s)，
 * 千兆网卡达到 50-80MB/s。
 */
object TransferConfig {

    // ==================== Socket 缓冲区 ====================

    /**
     * Socket 发送缓冲区: 512KB
     *
     * 默认 Android 约 64-128KB，对于 LAN 大文件传输太小。
     * 512KB 可以让内核攒足够的数据后一次性推送到网卡，
     * 减少 syscall 次数并充分利用 TCP 窗口。
     *
     * 设太大（如 4MB）无意义，LAN 的 BDP（带宽×延迟）很小:
     * 1Gbps × 1ms = 125KB，512KB 已经绰绰有余。
     */
    const val SOCKET_SEND_BUFFER = 512 * 1024       // 512KB

    /**
     * Socket 接收缓冲区: 512KB
     *
     * 与发送端对称，确保接收窗口不成为瓶颈。
     */
    const val SOCKET_RECV_BUFFER = 512 * 1024       // 512KB

    // ==================== 流缓冲区 ====================

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

    // ==================== 文件分片 ====================

    /**
     * 文件分片大小: 256KB
     *
     * 256KB 平衡了:
     * - 传 100MB = 400 个 chunk，开销降 4 倍
     * - 每片内存占用仅 256KB，对 Android 无压力
     * - 与 stream buffer 对齐，一个 chunk 刚好填满 buffer 后 flush
     *
     * 注意: chunk 过大（如 1MB）会增加单片重传代价和内存占用，
     * 且在 WiFi 抖动时可能导致写阻塞时间过长。
     */
    const val FILE_CHUNK_SIZE = 256 * 1024          // 256KB
    const val FILE_CHUNK_SIZE_BT = 64 * 1024        // 64KB

    // ==================== Packet 协议 ====================

    /**
     * 单个 Packet 最大长度: 1MB
     *
     * FILE_CHUNK 最大 256KB，加上其他消息类型（JSON）一般 < 10KB，
     * 1MB 留足余量。超过此值视为异常数据，拒绝接收防止内存攻击。
     */
    const val MAX_PACKET_LENGTH = 1 * 1024 * 1024   // 1MB

    // ==================== 连接 ====================

    const val CONNECT_TIMEOUT = 10_000              // TCP 连接超时
    const val HANDSHAKE_TIMEOUT = 10_000            // 握手阶段读超时

    // ==================== 心跳 ====================

    const val PING_INTERVAL = 15_000L               // Ping 发送间隔
    const val PONG_TIMEOUT = 20_000L                // Pong 超时阈值

    // ==================== 进度回调 ====================

    /**
     * 进度回调间隔阈值: 每传输 1MB 回调一次
     *
     * 避免每个 chunk 都触发 UI 更新（256KB × 4 = 1MB 回调一次），
     * 降低日志噪音和 UI 刷新频率。
     */
    const val PROGRESS_REPORT_INTERVAL = 1024 * 1024L   // 1MB
    const val PROGRESS_REPORT_INTERVAL_BT = 1024 * 64L  // 64KB (蓝牙模式下)
}