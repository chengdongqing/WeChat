package top.chengdongqing.wechat.features.contacts.ui.add.nfc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.model.ContactAddSource
import top.chengdongqing.wechat.features.contacts.domain.repository.AddFriendRepository
import javax.inject.Inject

@HiltViewModel
class NfcAddContactViewModel @Inject constructor(
    private val addFriendRepository: AddFriendRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcAddContactUiState())
    val uiState = _uiState.asStateFlow()

    fun handleNfcDetected(userId: String, onNavigateToContact: () -> Unit) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            addFriendRepository.fetchProfile(userId, ContactAddSource.Bump)?.let {
                onNavigateToContact()

                _uiState.update { it.copy(isLoading = false) }
            } ?: _uiState.update {
                it.copy(
                    isLoading = false,
                    error = context.getString(R.string.add_contact_fetch_profile_failed)
                )
            }
        }
    }
}

data class NfcAddContactUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)