package top.chengdongqing.wechat.features.contacts.ui.addfriend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.contacts.domain.usecase.QRCodeUseCase
import top.chengdongqing.wechat.features.me.repository.ProfileRepository
import javax.inject.Inject

data class AddFriendUiState(
    val qrCode: String = "",
    val wxId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val qrCodeUseCase: QRCodeUseCase,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AddFriendNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadMyProfile()
    }

    /**
     * 加载我的资料并生成二维码
     */
    private fun loadMyProfile() {
        viewModelScope.launch {
            val profileDeferred = async {
                profileRepository.getCurrentProfile().first()
            }
            val qrCodeDeferred = async {
                qrCodeUseCase.generateMyQRCode()
            }
            val profile = profileDeferred.await()
            val qrResult = qrCodeDeferred.await()

            _uiState.update { currentState ->
                qrResult.fold(
                    onSuccess = { qrCode ->
                        currentState.copy(
                            wxId = profile?.id ?: "",
                            qrCode = qrCode
                        )
                    },
                    onFailure = { e ->
                        currentState.copy(
                            wxId = profile?.id ?: "",
                            error = "生成二维码失败: ${e.message}"
                        )
                    }
                )
            }
        }
    }

    /**
     * 处理扫描到的二维码
     */
    fun handleScannedQRCode(qrContent: String) {
        viewModelScope.launch {
            Log.d("AddFriend", "开始处理二维码")
            _uiState.update { it.copy(isLoading = true, error = null) }

            qrCodeUseCase.scanQRCodeToAddFriend(qrContent).fold(
                onSuccess = { contact ->
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvent.emit(
                        AddFriendNavigationEvent.NavigateToContactDetail(contact.id)
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "连接失败"
                        )
                    }
                }
            )
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

sealed class AddFriendNavigationEvent {
    data class NavigateToContactDetail(val contactId: String) : AddFriendNavigationEvent()
}