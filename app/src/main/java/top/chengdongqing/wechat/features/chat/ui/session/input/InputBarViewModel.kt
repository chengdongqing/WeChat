package top.chengdongqing.wechat.features.chat.ui.session.input

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import top.chengdongqing.wechat.core.file.PrivateFileManager

@HiltViewModel
class InputBarViewModel @Inject constructor(
    val privateFileManager: PrivateFileManager
) : ViewModel()