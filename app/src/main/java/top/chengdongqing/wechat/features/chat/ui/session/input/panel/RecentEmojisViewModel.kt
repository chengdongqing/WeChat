package top.chengdongqing.wechat.features.chat.ui.session.input.panel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import top.chengdongqing.wechat.features.chat.data.store.RecentEmojisStore

@HiltViewModel
class RecentEmojisViewModel @Inject constructor(
    val store: RecentEmojisStore
) : ViewModel()