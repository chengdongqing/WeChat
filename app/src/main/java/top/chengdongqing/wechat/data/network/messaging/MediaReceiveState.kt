package top.chengdongqing.wechat.data.network.messaging

import top.chengdongqing.wechat.data.network.model.ChatProtocol

/**
 * 单个媒体文件的接收状态
 *
 * 以 userId 为 key 存储在 MessageReceiver 中，
 * 同一连接同一时刻只能接收一个媒体文件。
 *
 * 与旧版不同：不再持有 OutputStream，改为通过 [ChunkStorageManager] 分片写入磁盘。
 */
data class MediaReceiveState(
    /** 文件元数据 */
    val metadata: ChatProtocol.MediaMessage,

    /** 已接收字节数 */
    var receivedBytes: Long = 0,

    /** 上次上报进度时的字节数（节流用） */
    var lastReportedAt: Long = 0,

    /** 大文件写入会话（持有 FileChannel 句柄） */
    val writeSession: WriteSession? = null
) {
    /**
     * 关闭写入会话
     */
    fun closeSession() {
        writeSession?.close()
    }
}