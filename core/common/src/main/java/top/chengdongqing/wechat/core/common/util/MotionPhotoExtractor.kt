package top.chengdongqing.wechat.core.common.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

/**
 * 实况照片识别与读取
 */
object MotionPhotoExtractor {

    // 常见的视频起始特征码
    private val FTYP_MARKER = "ftyp".toByteArray()

    // 常见的 MP4 格式品牌标识，用于二次校验，防止误判
    private val VALID_BRANDS = listOf("mp42", "isom", "avc1", "qt  ", "heic")

    /**
     * 提取动态照片视频
     * @return 提取成功后的 File 对象，失败返回 null
     */
    fun extractVideo(context: Context, uri: Uri): File? {
        val cacheFile = File(context.cacheDir, "motion_${System.currentTimeMillis()}.mp4")

        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val channel = fis.channel
                    val fileSize = channel.size()

                    // 策略 1: 尝试通过官方 ExifInterface 获取 XMP 偏移量 (最准确)
                    val offset = getOffsetFromXmp(context, uri, fileSize)

                    // 策略 2: 如果 XMP 失败，尝试搜索文件末尾 (兼容旧版或部分厂商)
                    val finalOffset = offset ?: findVideoOffsetByScanning(channel, fileSize)

                    if (finalOffset != null && finalOffset > 0) {
                        val videoLength = fileSize - finalOffset
                        FileOutputStream(cacheFile).use { fos ->
                            channel.transferTo(finalOffset, videoLength, fos.channel)
                        }
                        cacheFile
                    } else {
                        null
                    }
                }
            }
        }.getOrNull()
    }

    /**
     * 从 XMP 元数据中获取偏移量
     * Google/Pixel 标准：MicroVideoOffset 指的是从文件末尾向前的偏移量
     */
    private fun getOffsetFromXmp(context: Context, uri: Uri, fileSize: Long): Long? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                // 常见的两个 XMP 标签
                val offset = exif.getAttribute(ExifInterface.TAG_XMP)?.let { xmp ->
                    if (xmp.contains("MicroVideoOffset")) {
                        // 提取 <GCamera:MicroVideoOffset> 数值
                        val regex = "MicroVideoOffset=\"(\\d+)\"".toRegex()
                        regex.find(xmp)?.groupValues?.get(1)?.toLongOrNull()
                    } else null
                }
                // 返回绝对位置：总长度 - 视频相对于结尾的偏移
                offset?.let { fileSize - it }
            }
        }.getOrNull()
    }

    /**
     * 增强版暴力搜索：加入 Brand 校验防止误判
     */
    private fun findVideoOffsetByScanning(channel: FileChannel, size: Long): Long? {
        // 动态照片视频通常在末尾，只扫描最后 2MB 即可，兼顾性能和准确性
        val scanRange = 2 * 1024 * 1024L
        val startPos = if (size > scanRange) size - scanRange else 0L
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, startPos, size - startPos)

        // 倒序搜索 ftyp
        for (i in (buffer.limit() - 8) downTo 4) {
            // 匹配 "ftyp"
            if (buffer.get(i) == FTYP_MARKER[0] &&
                buffer.get(i + 1) == FTYP_MARKER[1] &&
                buffer.get(i + 2) == FTYP_MARKER[2] &&
                buffer.get(i + 3) == FTYP_MARKER[3]
            ) {
                // 读取紧随其后的 Brand (4字节) 进行二次校验
                val brandBytes = ByteArray(4)
                buffer.position(i + 4)
                buffer.get(brandBytes)
                val brand = String(brandBytes).trim()

                if (VALID_BRANDS.contains(brand)) {
                    // 找到了，返回绝对位置 (减去 ftyp 前面的 4 字节 size 字段)
                    return startPos + i - 4
                }
            }
        }
        return null
    }
}