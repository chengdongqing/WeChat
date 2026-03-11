package top.chengdongqing.wechat.features.settings.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val repository: ChatSettingsRepository
) : ViewModel() {

    val speakerEnabled = repository.speakerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val sendButtonEnabled = repository.sendButtonEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val e2eEnabled = repository.e2eEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleSpeaker(enabled: Boolean) {
        viewModelScope.launch { repository.toggleSpeaker(!enabled) }
    }

    fun toggleSendButton(enabled: Boolean) {
        viewModelScope.launch { repository.toggleSendButton(enabled) }
    }

    fun toggleE2e(enabled: Boolean) {
        viewModelScope.launch { repository.toggleE2e(enabled) }
    }
}