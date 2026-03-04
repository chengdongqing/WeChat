package top.chengdongqing.wechat.core.util

import top.chengdongqing.wechat.data.model.MessageType

/**
 * 文件配置
 */
data class FileConfig(
    val dirName: String,
    val prefix: String,
    val extension: String
)

/**
 * 获取文件配置
 */
fun MessageType.getFileConfig(): FileConfig {
    return when (this) {
        MessageType.Image,
        MessageType.Sticker -> FileConfig("images", "IMG", "jpg")

        MessageType.Video -> FileConfig("videos", "VID", "mp4")
        MessageType.Voice -> FileConfig("audios", "RCD", "m4a")
        else -> FileConfig("files", "FILE", "bin")
    }
}

/**
 * 生成文件名
 *
 * @param prefix 文件前缀（如 IMG, VID）
 * @param extension 文件扩展名（会自动去除前导点）
 * @param timestamp 时间戳（默认当前时间）
 */
fun generateFileName(
    prefix: String,
    extension: String,
    timestamp: Long = System.currentTimeMillis()
): String {
    return "${prefix}_${timestamp}.${extension.trimStart('.')}"
}

/**
 * 从文件名或路径提取扩展名
 */
fun String?.extractExtension(): String? {
    if (isNullOrBlank()) return null
    val lastDotIndex = lastIndexOf('.')
    if (lastDotIndex == -1 || lastDotIndex == length - 1) {
        return null
    }
    return substring(lastDotIndex + 1)
}