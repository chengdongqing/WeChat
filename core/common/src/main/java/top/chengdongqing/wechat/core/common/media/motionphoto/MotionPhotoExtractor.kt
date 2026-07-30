package top.chengdongqing.wechat.core.common.media.motionphoto

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets

/**
 * 实况照片识别与读取
 */
object MotionPhotoExtractor {

    // 常见的视频起始特征码
    private val FTYP_MARKER = "ftyp".toByteArray()

    // 常见的 MP4 格式品牌标识，用于二次校验，防止误判
    private val VALID_BRANDS = setOf("isom", "iso2", "mp41", "mp42", "avc1", "qt  ")

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

                    // 策略 1: 优先使用 XMP 中声明的视频长度
                    val offset = getOffsetFromXmp(context, uri, fileSize)
                        ?.takeIf { isVideoStart(channel, it, fileSize) }

                    // 策略 2: XMP 缺失或不规范时，分块搜索完整文件
                    val finalOffset = offset ?: findVideoOffsetByScanning(channel, fileSize)

                    if (finalOffset != null) {
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
        }.getOrNull().also { result ->
            if (result == null) cacheFile.delete()
        }
    }

    /**
     * 从 XMP 元数据中获取视频的绝对起始位置。
     *
     * Motion Photo 1.0 使用 MotionPhoto Item 的 Length；旧版使用 MicroVideoOffset。
     * 两者的值都可以视为从文件末尾向前的视频长度。
     */
    private fun getOffsetFromXmp(context: Context, uri: Uri, fileSize: Long): Long? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val xmp = exif.getAttribute(ExifInterface.TAG_XMP) ?: return@use null
                val videoLength = getMotionPhotoLength(xmp) ?: getMicroVideoOffset(xmp)
                videoLength
                    ?.takeIf { it in 1 until fileSize }
                    ?.let { fileSize - it }
            }
        }.getOrNull()
    }

    /**
     * 读取 Motion Photo 1.0 Container Directory 中视频项的长度。
     */
    private fun getMotionPhotoLength(xmp: String): Long? {
        return START_TAG_REGEX.findAll(xmp)
            .map { it.value }
            .firstOrNull { tag ->
                getAttribute(tag, "Semantic").equals("MotionPhoto", ignoreCase = true) &&
                    getAttribute(tag, "Mime")?.let {
                        it.equals("video/mp4", ignoreCase = true) ||
                            it.equals("video/quicktime", ignoreCase = true)
                    } == true
            }
            ?.let { getAttribute(it, "Length")?.toLongOrNull() }
    }

    private fun getMicroVideoOffset(xmp: String): Long? {
        return MICRO_VIDEO_OFFSET_ATTRIBUTE.find(xmp)?.groupValues?.get(2)?.toLongOrNull()
            ?: MICRO_VIDEO_OFFSET_ELEMENT.find(xmp)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun getAttribute(tag: String, localName: String): String? {
        val regex = Regex(
            """(?:[\w-]+:)?$localName\s*=\s*(["'])(.*?)\1""",
            RegexOption.IGNORE_CASE
        )
        return regex.find(tag)?.groupValues?.get(2)
    }

    /**
     * 分块倒序搜索完整文件。扫描内存固定在约 2MB，不再受视频大小限制。
     */
    private fun findVideoOffsetByScanning(channel: FileChannel, size: Long): Long? {
        var endPos = size

        while (endPos > 0) {
            val startPos = (endPos - SCAN_CHUNK_SIZE).coerceAtLeast(0)
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, startPos, endPos - startPos)

            for (i in (buffer.limit() - 8) downTo 4) {
                if (buffer.get(i) == FTYP_MARKER[0] &&
                    buffer.get(i + 1) == FTYP_MARKER[1] &&
                    buffer.get(i + 2) == FTYP_MARKER[2] &&
                    buffer.get(i + 3) == FTYP_MARKER[3]
                ) {
                    val brandBytes = ByteArray(4)
                    buffer.position(i + 4)
                    buffer.get(brandBytes)
                    val brand = String(brandBytes, StandardCharsets.US_ASCII)
                    val offset = startPos + i - 4

                    if (brand in VALID_BRANDS && offset > 0) {
                        return offset
                    }
                }
            }

            if (startPos == 0L) break
            endPos = startPos + SCAN_OVERLAP
        }
        return null
    }

    private fun isVideoStart(channel: FileChannel, offset: Long, fileSize: Long): Boolean {
        if (offset <= 0 || offset + VIDEO_HEADER_SIZE > fileSize) return false

        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, VIDEO_HEADER_SIZE.toLong())
        if (buffer.get(4) != FTYP_MARKER[0] ||
            buffer.get(5) != FTYP_MARKER[1] ||
            buffer.get(6) != FTYP_MARKER[2] ||
            buffer.get(7) != FTYP_MARKER[3]
        ) {
            return false
        }

        val brandBytes = ByteArray(4)
        buffer.position(8)
        buffer.get(brandBytes)
        return String(brandBytes, StandardCharsets.US_ASCII) in VALID_BRANDS
    }

    private val START_TAG_REGEX = Regex("""<[^!?/][^>]*>""")
    private val MICRO_VIDEO_OFFSET_ATTRIBUTE =
        Regex("""MicroVideoOffset\s*=\s*(["'])(\d+)\1""", RegexOption.IGNORE_CASE)
    private val MICRO_VIDEO_OFFSET_ELEMENT =
        Regex("""<[^>]*MicroVideoOffset[^>]*>\s*(\d+)\s*</""", RegexOption.IGNORE_CASE)

    private const val SCAN_CHUNK_SIZE = 2 * 1024 * 1024L
    private const val SCAN_OVERLAP = 12L
    private const val VIDEO_HEADER_SIZE = 12
}
