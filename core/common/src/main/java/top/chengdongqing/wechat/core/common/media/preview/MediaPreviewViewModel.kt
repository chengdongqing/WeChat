package top.chengdongqing.wechat.core.common.media.preview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.file.PublicFileManager
import top.chengdongqing.wechat.core.common.file.shareUri
import top.chengdongqing.wechat.core.common.media.model.MediaItem
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.util.showToast
import javax.inject.Inject
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@HiltViewModel
class MediaPreviewViewModel @Inject constructor(
    private val publicFileManager: PublicFileManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * 保存媒体文件
     */
    fun saveMedia(media: MediaItem) {
        viewModelScope.launch {
            val res = publicFileManager.saveMedia(
                messageType = if (media.isImage) MessageType.Image else MessageType.Video,
                sourceUri = media.uri,
                filename = media.filename
            )
            if (res != null) {
                context.showToast(context.getString(DesignR.string.msg_save_success))
            } else {
                context.showToast(context.getString(DesignR.string.msg_save_failed))
            }
        }
    }

    /**
     * 分享媒体文件
     */
    fun shareMedia(media: MediaItem) {
        context.shareUri(media.uri, media.mimeType)
    }
}