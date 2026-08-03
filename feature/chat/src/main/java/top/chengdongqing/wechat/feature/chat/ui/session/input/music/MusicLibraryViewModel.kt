package top.chengdongqing.wechat.feature.chat.ui.session.input.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.feature.chat.data.store.MusicLibraryStore
import javax.inject.Inject

@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val store: MusicLibraryStore
) : ViewModel() {

    fun addFiles(track: MusicTrack) {
        viewModelScope.launch(Dispatchers.IO) { store.add(track) }
    }

    /**
     * 曲库只拥有自己的文件入口。历史消息仍引用音频时不能物理删除；
     * 最后一条消息删除后会由 AssetReferenceManager 统一清理。
     */
    fun deleteFiles(track: MusicTrack) {
        viewModelScope.launch(Dispatchers.IO) {
            store.delete(track)
        }
    }
}
