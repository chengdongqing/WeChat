package top.chengdongqing.wechat.feature.settings.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.PrivacySettingsRepository

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val repository: PrivacySettingsRepository
) : ViewModel() {

    val friendVerifyEnabled = repository.friendVerifyEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleFriendVerify(enabled: Boolean) {
        viewModelScope.launch { repository.toggleFriendVerify(enabled) }
    }
}