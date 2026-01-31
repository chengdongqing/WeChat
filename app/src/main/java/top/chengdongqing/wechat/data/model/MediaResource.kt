package top.chengdongqing.wechat.data.model

import android.net.Uri
import java.io.File

data class MediaResource(
    val file: File,
    val filename: String,
    val mimeType: String,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Long = 0,
    val thumbBase64: String? = null
) {
    fun toMediaItem(uri: Uri, mediaType: MediaType = MediaType.IMAGE) = MediaItem(
        uri = uri,
        filename = filename,
        mediaType = mediaType,
        mimeType = mimeType,
        width = width,
        height = height,
        duration = duration
    )
}