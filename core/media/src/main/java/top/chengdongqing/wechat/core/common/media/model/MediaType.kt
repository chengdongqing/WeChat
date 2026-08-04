package top.chengdongqing.wechat.core.common.media.model

import android.provider.MediaStore

enum class MediaType(val columnType: Int) {
    Image(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
    Video(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    Audio(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO),
    Recording(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO);

    companion object {
        fun ofColumnType(columnType: Int): MediaType? {
            return entries.find { it.columnType == columnType }
        }
    }
}

enum class VisualMediaType {
    Image,
    Video,
    ImageAndVideo
}
