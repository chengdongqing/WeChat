package top.chengdongqing.wechat.data.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    val uri: Uri,
    val filename: String,
    val mediaType: MediaType,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val duration: Long = 0,
    val date: Long = 0
) : Parcelable {
    val isImage get() = mediaType === MediaType.Image
    val isVideo get() = mediaType === MediaType.Video
}