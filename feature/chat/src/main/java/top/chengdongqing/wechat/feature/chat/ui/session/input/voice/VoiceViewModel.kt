package top.chengdongqing.wechat.feature.chat.ui.session.input.voice

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    val audioRecorder: AudioRecorderManager
) : ViewModel()