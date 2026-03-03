package top.chengdongqing.wechat.core.designsystem.components.media.preview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.file.MediaStoreManager
import top.chengdongqing.wechat.core.util.shareUri
import top.chengdongqing.wechat.core.util.showToast
import javax.inject.Inject

@HiltViewModel
class MediaPreviewViewModel @Inject constructor(
    private val mediaStoreManager: MediaStoreManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * 保存媒体文件
     */
    fun saveMedia(media: MediaItem) {
        viewModelScope.launch {
            val success = mediaStoreManager.saveToAlbum(
                sourceUri = media.uri,
                filename = media.filename,
                mimeType = media.mimeType,
                mediaType = media.mediaType
            )
            if (success) {
                context.showToast("已保存到相册")
            } else {
                context.showToast("保存失败")
            }
        }
    }

    /**
     * 分享媒体文件
     */
    fun shareMedia(media: MediaItem) {
        context.shareUri(media.uri, media.mimeType, "分享媒体文件")
    }
}