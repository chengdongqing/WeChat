package top.chengdongqing.wechat.core.common.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.R
import java.io.File
import java.io.FileOutputStream

/**
 * 获取 FileProvider Uri
 *
 * 用于跨应用共享文件
 */
fun Context.getFileProviderUri(file: File): Uri {
    return FileProvider.getUriForFile(this, "$packageName.provider", file)
}

/**
 * 删除临时文件
 */
suspend fun Context.deleteFileByUri(uri: Uri) = withContext(Dispatchers.IO) {
    contentResolver.delete(uri, null, null)
}

/**
 * 创建一个临时的空媒体文件 Uri（用于拍照/录像等）
 */
private suspend fun Context.createMediaUri(isVideo: Boolean = false): Uri =
    withContext(Dispatchers.IO) {
        val prefix = if (isVideo) "VID_" else "IMG_"
        val suffix = if (isVideo) ".mp4" else ".jpg"
        val file = File.createTempFile(prefix, suffix)

        getFileProviderUri(file)
    }

/**
 * 创建图片 Uri
 */
suspend fun Context.createImageUri(): Uri =
    createMediaUri(isVideo = false)

/**
 * 创建视频 Uri
 */
suspend fun Context.createVideoUri(): Uri =
    createMediaUri(isVideo = true)

/**
 * 从 Bitmap 创建临时 Uri
 *
 * @param bitmap 位图
 * @param quality 压缩质量（0-100）
 * @return FileProvider Uri
 */
suspend fun Context.createImageUri(
    bitmap: Bitmap,
    quality: Int = 90
): Uri = withContext(Dispatchers.IO) {
    val tempFile = File.createTempFile("IMG_", ".jpg")

    FileOutputStream(tempFile).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    }

    getFileProviderUri(tempFile)
}

/**
 * 分享 Uri
 */
fun Context.shareUri(
    uri: Uri,
    mimeType: String,
    title: String = getString(R.string.action_share)
) {
    val shareUri = if (uri.scheme == "file") {
        getFileProviderUri(File(uri.path!!))
    } else {
        uri
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, shareUri)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    startActivity(Intent.createChooser(intent, title).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}

/**
 * 打开文件
 *
 * @param file 要打开的文件
 * @param mimeType MIME 类型
 * @param showChooser 是否显示选择器
 */
fun Context.openFile(
    file: File,
    mimeType: String,
    showChooser: Boolean = true
) {
    val uri = getFileProviderUri(file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val finalIntent = if (showChooser) {
        Intent.createChooser(intent, "打开文件").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    } else {
        intent
    }

    startActivity(finalIntent)
}


/**
 * 将字符串转换为 Asset 路径
 *
 * 用于在 WebView 或 Coil 中加载 Asset 资源
 *
 * 示例：
 * ```
 * val path = "images/logo.png".asAssetPath
 * // 结果：file:///android_asset/images/logo.png
 * ```
 */
val String.asAssetPath: String
    get() = "file:///android_asset/$this"