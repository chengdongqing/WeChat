package top.chengdongqing.wechat.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.data.model.MessageType
import java.io.File

/**
 * 获取 FileProvider Uri
 *
 * 用于跨应用共享文件
 */
fun Context.getFileProviderUri(file: File): Uri {
    return FileProvider.getUriForFile(this, "$packageName.provider", file)
}

/**
 * 创建媒体文件 Uri（用于相机/录像）
 *
 * 创建一个空文件并返回其 FileProvider Uri
 *
 * @param isVideo 是否为视频（false 为图片）
 * @return FileProvider Uri
 */
private suspend fun Context.createMediaUri(isVideo: Boolean = false): Uri =
    withContext(Dispatchers.IO) {
        val messageType = if (isVideo) MessageType.Video else MessageType.Image
        val config = FileNameUtils.getFileConfig(messageType)

        val fileName = FileNameUtils.generateFileName(config.prefix, config.extension)
        val file = File(filesDir, "${config.dirName}/$fileName").apply {
            parentFile?.mkdirs()
        }

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
 * 分享文件
 *
 * @param file 要分享的文件
 * @param mimeType MIME 类型
 * @param title 分享标题
 */
fun Context.shareFile(
    file: File,
    mimeType: String,
    title: String = "分享文件"
) {
    val uri = getFileProviderUri(file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    startActivity(Intent.createChooser(intent, title).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}

/**
 * 分享 Uri
 */
fun Context.shareUri(
    uri: Uri,
    mimeType: String,
    title: String = "分享文件"
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