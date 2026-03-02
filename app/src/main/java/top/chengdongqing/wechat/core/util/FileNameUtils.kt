package top.chengdongqing.wechat.core.util

import top.chengdongqing.wechat.data.database.entity.MessageType

/**
 * 文件命名工具
 * 统一管理所有文件的命名规则和配置
 */
object FileNameUtils {
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
    fun getFileConfig(messageType: MessageType): FileConfig {
        return when (messageType) {
            MessageType.Image -> FileConfig("images", "IMG", getImageExtension())
            MessageType.Video -> FileConfig("videos", "VID", "mp4")
            MessageType.Voice -> FileConfig("audios", "RCD", "m4a")
            MessageType.File -> FileConfig("files", "FILE", "bin")
            else -> FileConfig("files", "FILE", "bin")
        }
    }

    /**
     * 获取图片默认扩展名
     *
     * Android 10+ 相机可能使用 HEIC 格式，但为了兼容性统一使用 JPG
     * 实际保存时会根据源文件检测真实格式
     */
    private fun getImageExtension(): String = "jpg"

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
     * 检测图片实际格式
     *
     * 从文件头魔数判断真实格式，比 MIME 类型更可靠
     */
    fun detectImageFormat(file: java.io.File): String {
        return try {
            val bytes = file.inputStream().use {
                it.readNBytes(12)
            }

            when {
                // JPEG: FF D8 FF
                bytes.size >= 3 &&
                        bytes[0] == 0xFF.toByte() &&
                        bytes[1] == 0xD8.toByte() &&
                        bytes[2] == 0xFF.toByte() -> "jpg"

                // PNG: 89 50 4E 47
                bytes.size >= 4 &&
                        bytes[0] == 0x89.toByte() &&
                        bytes[1] == 0x50.toByte() &&
                        bytes[2] == 0x4E.toByte() &&
                        bytes[3] == 0x47.toByte() -> "png"

                // HEIC/HEIF: ftyp heic/heix/hevc/heim/heis/hevm/hevs
                bytes.size >= 12 &&
                        bytes[4] == 0x66.toByte() && // 'f'
                        bytes[5] == 0x74.toByte() && // 't'
                        bytes[6] == 0x79.toByte() && // 'y'
                        bytes[7] == 0x70.toByte() -> { // 'p'
                    val subtype = String(bytes, 8, 4, Charsets.UTF_8)
                    if (subtype.startsWith("hei") || subtype.startsWith("hev")) {
                        "heic"
                    } else {
                        "jpg"
                    }
                }

                // WebP: RIFF....WEBP
                bytes.size >= 12 &&
                        bytes[0] == 0x52.toByte() && // 'R'
                        bytes[1] == 0x49.toByte() && // 'I'
                        bytes[2] == 0x46.toByte() && // 'F'
                        bytes[3] == 0x46.toByte() -> "webp" // 'F'

                else -> "jpg"
            }
        } catch (e: Exception) {
            "jpg"
        }
    }

    /**
     * 从文件名或路径提取扩展名
     */
    fun extractExtension(filename: String?): String? {
        if (filename.isNullOrBlank()) return null
        val lastDotIndex = filename.lastIndexOf('.')
        if (lastDotIndex == -1 || lastDotIndex == filename.length - 1) {
            return null
        }
        return filename.substring(lastDotIndex + 1)
    }
}