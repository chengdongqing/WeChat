package top.chengdongqing.wechat.feature.contacts.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.qrcode.QRCodeResult
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.data.usecase.QRCodeUseCase
import top.chengdongqing.wechat.core.model.UserProfile
import javax.inject.Inject

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val qrCodeUseCase: QRCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddFriendEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
    }

    fun generateMyQrCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            qrCodeUseCase.generateMyQRCode()
                .onSuccess { qrCode -> _uiState.update { it.copy(qrCode = qrCode) } }
                .onFailure { _events.emit(AddFriendEvent.ShowError(it.message ?: "二维码生成失败")) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun handleScannedQrCode(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = qrCodeUseCase.handleScannedQRCode(content)) {
                is QRCodeResult.AddFriend -> _events.emit(AddFriendEvent.NavigateToContact(result.contactId))
                is QRCodeResult.OpenUrl -> _events.emit(AddFriendEvent.OpenUrl(result.url))
                is QRCodeResult.ShowText -> _events.emit(AddFriendEvent.ShowText(result.text))
                is QRCodeResult.JoinGroup -> _events.emit(AddFriendEvent.ShowError("群聊功能开发中"))
                is QRCodeResult.Error -> _events.emit(AddFriendEvent.ShowError(result.message))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

data class AddFriendUiState(
    val profile: UserProfile? = null,
    val qrCode: String = "",
    val isLoading: Boolean = false
)

sealed interface AddFriendEvent {
    data class NavigateToContact(val contactId: String) : AddFriendEvent
    data class OpenUrl(val url: String) : AddFriendEvent
    data class ShowText(val text: String) : AddFriendEvent
    data class ShowError(val message: String) : AddFriendEvent
}
