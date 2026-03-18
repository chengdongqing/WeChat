package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import top.chengdongqing.wechat.core.file.PrivateFileManager

@HiltViewModel
class HandlerViewModel @Inject constructor(
    val privateFileManager: PrivateFileManager
) : ViewModel()