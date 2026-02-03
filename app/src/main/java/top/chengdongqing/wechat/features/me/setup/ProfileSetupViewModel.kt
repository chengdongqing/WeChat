package top.chengdongqing.wechat.features.me.setup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val deviceId: String = "",
    val userName: String = "",
    val userNameError: String? = null,
    val avatarUri: Uri? = null,
    val isSaving: Boolean = false,
    val isValid: Boolean = false
)

class ProfileSetupViewModel(
    application: Application,
//    private val repository: ProfileRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        ProfileSetupUiState(/*deviceId = repository.getDeviceId()*/)
    )
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun onUserNameChange(name: String) {
        _uiState.update { state ->
            state.copy(
                userName = name,
                userNameError = validateUserName(name),
                isValid = validateUserName(name) == null && name.isNotBlank()
            )
        }
    }


    fun onAvatarSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                avatarUri = uri
            )
        }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
//                repository.updateLocalProfile(
//                    userName = state.userName,
//                    avatarUri = state.avatarUri,
//                    peers = emptyList() // 首次设置时还没有对等设备
//                )
                // 保存成功，由 onSetupComplete 回调处理导航
            } catch (e: Exception) {
                // 处理错误
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        userNameError = "保存失败：${e.message}"
                    )
                }
            }
        }
    }

    private fun validateUserName(name: String): String? {
        return when {
            name.isBlank() -> "昵称不能为空"
            name.length < 2 -> "昵称至少2个字符"
            name.length > 20 -> "昵称最多20个字符"
            else -> null
        }
    }
}