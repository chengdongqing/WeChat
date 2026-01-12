package top.chengdongqing.wechat.core.util

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import top.chengdongqing.wechat.data.model.MediaResource
import java.io.File

/**
 * 媒体文件预处理
 * 返回文件元数据
 */
fun prepareMediaResource(context: Context, uri: Uri): MediaResource? {
    return try {
        val resolver = context.contentResolver

        // 获取基本信息
        val mimeType = resolver.getType(uri) ?: "image/jpeg"
        val fileName = context.getFileName(uri)

        // 拷贝到私有目录 (files/media),避免发送过程中被删除
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val targetFile = File(mediaDir, fileName)
        resolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        // 生成缩略图
        var thumbBase64: String? = null
        if (mimeType.startsWith("image/")) {
            val bitmap = BitmapFactory.decodeFile(targetFile.absolutePath)
            // 压缩成极小的缩略图 (比如 100x100)
            val thumbBitmap = ThumbnailUtils.extractThumbnail(bitmap, 100, 100)
            // 转为base64
            thumbBase64 = thumbBitmap.toBase64()
        }

        MediaResource(
            file = targetFile,
            fileName = fileName,
            mimeType = mimeType,
            size = targetFile.length(),
            thumbBase64 = thumbBase64
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 获取文件名
 */
private fun Context.getFileName(uri: Uri): String {
    var name = "IMG_${System.currentTimeMillis()}.jpg" // 默认兜底名称

    // 如果是 File 类型的 Uri，直接用路径里面的文件名
    if (uri.scheme == "file") {
        return uri.lastPathSegment ?: name
    }

    // 如果是 Content 类型的 Uri (相册、文件管理器)，去数据库查询文件名
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            val originalName = cursor.getString(nameIndex)
            if (!originalName.isNullOrBlank()) {
                name = originalName
            }
        }
    }
    return name
}