package top.chengdongqing.wechat.features.contacts.ui.addfriend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.contacts.data.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.me.repository.ProfileRepository
import javax.inject.Inject

data class AddFriendUiState(
    val myQRCode: String = "",
    val myUserId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val contactP2PRepository: ContactP2PRepository,
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
            profileRepository.getCurrentProfile().collect { profile ->
                if (profile != null) {
                    // 生成二维码
                    val qrCode = contactP2PRepository.generateMyQRCode()

                    _uiState.update {
                        it.copy(
                            myQRCode = qrCode,
                            myUserId = profile.id
                        )
                    }
                }
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

            val result = contactP2PRepository.handleScannedQRCode(qrContent)

            result.fold(
                onSuccess = { contact ->
                    Log.d("AddFriend", "成功: ${contact.name}")
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvent.emit(
                        AddFriendNavigationEvent.NavigateToContactDetail(contact.id)
                    )
                },
                onFailure = { error ->
                    Log.e("AddFriend", "失败: ${error.message}", error)
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