package top.chengdongqing.wechat.core.network.messaging

import top.chengdongqing.wechat.core.data.model.ChatProtocol
import java.io.BufferedOutputStream
import java.io.File

/**
 * 单个文件接收状态
 */
data class ReceiveState(
    /**
     * 文件元数据
     */
    val metadata: ChatProtocol.MediaMessage,

    /**
     * 已接收字节数
     */
    var receivedBytes: Long = 0,

    /**
     * 上次上报进度时的字节数（节流用）
     */
    var lastReportedAt: Long = 0,

    /**
     * 大文件写入会话（持有 FileChannel 句柄）
     */
    val writeSession: WriteSession? = null
) {
    /**
     * 关闭写入会话
     */
    fun closeSession() {
        writeSession?.close()
    }
}

/**
 * 媒体接收上下文
 */
data class ReceiveContext(
    val userId: String,
    val state: ReceiveState,
    val isLargeFile: Boolean,
    /** 小文件用：临时文件 */
    val tempFile: File? = null,
    /** 小文件用：写入流 */
    val outputStream: BufferedOutputStream? = null
) {
    fun cleanup() {
        // 小文件：关闭流 + 删除临时文件
        runCatching { outputStream?.close() }
        runCatching { tempFile?.delete() }
        // 大文件：关闭 FileChannel
        state.closeSession()
    }
}