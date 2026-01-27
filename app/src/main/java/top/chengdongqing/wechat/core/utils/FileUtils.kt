package top.chengdongqing.wechat.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import top.chengdongqing.wechat.data.model.MediaResource
import java.io.File
import java.io.FileOutputStream

/**
 * 媒体文件预处理
 * 返回文件元数据
 */
fun prepareMediaResource(context: Context, uri: Uri): MediaResource? {
    return try {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/jpeg"

        // 尝试从数据库一次性获取元数据（文件名、宽高、时长）
        val meta = queryMediaMetadata(context, uri)
        val fileName = meta.name ?: "FILE_${System.currentTimeMillis()}"

        // 拷贝到应用私有目录，确保传输稳定性
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val targetFile = File(mediaDir, fileName)
        resolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        var width = meta.width
        var height = meta.height

        // 兜底策略：如果数据库信息不全，则解析物理文件
        val isImage = mimeType.startsWith("image/")
        if (isImage && width <= 0) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(targetFile.absolutePath, options)
                width = options.outWidth
                height = options.outHeight
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        MediaResource(
            file = targetFile,
            filename = fileName,
            mimeType = mimeType,
            size = targetFile.length(),
            width = width,
            height = height,
            duration = meta.duration
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 核心查询逻辑：合并所有字段查询
 */
private data class TempMeta(val name: String?, val width: Int, val height: Int, val duration: Long)

private fun queryMediaMetadata(context: Context, uri: Uri): TempMeta {
    if (uri.scheme == "file") {
        return TempMeta(uri.lastPathSegment, 0, 0, 0)
    }

    val projection = arrayOf(
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.WIDTH,
        MediaStore.MediaColumns.HEIGHT,
        MediaStore.MediaColumns.DURATION
    )

    var name: String? = null
    var width = 0
    var height = 0
    var duration = 0L

    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val wIdx = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val hIdx = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val dIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

                if (nIdx != -1) name = cursor.getString(nIdx)
                if (wIdx != -1) width = cursor.getInt(wIdx)
                if (hIdx != -1) height = cursor.getInt(hIdx)
                if (dIdx != -1) duration = cursor.getLong(dIdx)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return TempMeta(name, width, height, duration)
}

/**
 * 分享文件
 */
fun Context.shareContent(content: Any, mimeType: String, title: String = "分享文件") {
    val shareUri: Uri = when (content) {
        is File -> getFileProviderUri(content)
        is Uri -> content
        else -> throw IllegalArgumentException("不支持的内容类型: ${content::class.java}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, shareUri)
        // 授予临时访问权限
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, title))
}

/**
 * 获取隔离的文件uri
 */
fun Context.getFileProviderUri(file: File): Uri {
    return FileProvider.getUriForFile(this, "$packageName.provider", file)
}

val String.asAssetPath: String
    get() = "file:///android_asset/$this"

/**
 * 保存截图到缓存
 */
fun Context.saveSnapshotToCache(bitmap: Bitmap): Uri? {
    return try {
        // 创建 snapshots 缓存文件夹
        val cachePath = File(cacheDir, "snapshots")
        if (!cachePath.exists()) cachePath.mkdirs()

        // 创建文件（以时间戳命名避免覆盖）
        val fileName = "MAP_SNAPSHOT_${System.currentTimeMillis()}.jpg"
        val file = File(cachePath, fileName)

        // 写入文件
        FileOutputStream(file).use { out ->
            // 使用 JPEG 格式，质量设为 80-90 即可，平衡体积和清晰度
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }

        // 通过 FileProvider 获取安全 Uri
        this.getFileProviderUri(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        // 回收 Bitmap 释放内存
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}