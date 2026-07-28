package top.chengdongqing.wechat.feature.chat.ui.session.input.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.core.database.dao.MessageDao
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val messageDao: MessageDao
) : ViewModel() {

    /**
     * 曲库只拥有自己的文件入口。历史消息仍引用音频时不能物理删除；
     * 最后一条消息删除后会沿用消息仓库的 FileReferenceManager 清理。
     */
    fun deleteFiles(track: MusicTrack) {
        viewModelScope.launch(Dispatchers.IO) {
            track.coverPath?.let { File(it).delete() }
            track.audioPath?.let { path ->
                if (!messageDao.hasLocalPathReference(path)) File(path).delete()
            }
        }
    }
}
