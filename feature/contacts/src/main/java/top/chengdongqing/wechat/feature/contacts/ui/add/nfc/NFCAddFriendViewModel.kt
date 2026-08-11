package top.chengdongqing.wechat.feature.contacts.ui.add.nfc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.AddFriendRepository
import top.chengdongqing.wechat.core.model.ContactAddSource
import javax.inject.Inject
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@HiltViewModel
class NFCAddFriendViewModel @Inject constructor(
    private val addFriendRepository: AddFriendRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NFCAddFriendUiState())
    val uiState = _uiState.asStateFlow()

    fun handleNfcDetected(userId: String, onContact: () -> Unit) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            addFriendRepository.fetchProfile(userId, ContactAddSource.Bump)?.let {
                onContact()

                _uiState.update { it.copy(isLoading = false) }
            } ?: _uiState.update {
                it.copy(
                    isLoading = false,
                    error = context.getString(DesignR.string.add_contact_fetch_profile_failed)
                )
            }
        }
    }
}

data class NFCAddFriendUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
