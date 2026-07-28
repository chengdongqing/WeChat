package top.chengdongqing.wechat.feature.chat.ui.session.input.panel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.feature.chat.data.store.ManagedSticker
import top.chengdongqing.wechat.feature.chat.data.store.StickerStore
import java.io.File
import javax.inject.Inject

@HiltViewModel
class StickersViewModel @Inject constructor(
    private val store: StickerStore,
    private val privateFileManager: PrivateFileManager
) : ViewModel() {
    val stickers = store.stickers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun add(uri: Uri) = viewModelScope.launch {
        privateFileManager.saveMedia(MessageType.Sticker, sourceUri = uri)
            .onSuccess { store.add(it) }
    }

    fun moveToFront(sticker: ManagedSticker) =
        viewModelScope.launch { store.moveToFront(sticker.path) }

    fun delete(sticker: ManagedSticker) = viewModelScope.launch {
        store.delete(sticker)
        if (!sticker.isAsset) File(sticker.path).delete()
    }
}
