package top.chengdongqing.wechat.features.contacts.ui.add.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.contacts.domain.repository.AddFriendRepository
import javax.inject.Inject

@HiltViewModel
class NfcAddContactViewModel @Inject constructor(
    private val addFriendRepository: AddFriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcAddContactUiState())
    val uiState = _uiState.asStateFlow()

    fun handleNfcDetected(userId: String, onNavigateToContact: () -> Unit) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            addFriendRepository.fetchProfile(userId)?.let { profile ->
                addFriendRepository.setContactToCache(userId, profile)
                onNavigateToContact()

                _uiState.update { it.copy(isLoading = false) }
            } ?: _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "获取对方资料失败"
                )
            }
        }
    }
}

data class NfcAddContactUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)