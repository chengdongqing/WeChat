package top.chengdongqing.wechat.features.me.ui.setup

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.manager.FileManager
import top.chengdongqing.wechat.data.model.UserProfile
import top.chengdongqing.wechat.features.me.repository.ProfileRepository
import javax.inject.Inject

data class ProfileSetupUiState(
    val nickname: String = "",
    val avatarUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val fileManager: FileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    /**
     * 更新昵称
     */
    fun updateNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname) }
    }

    /**
     * 更新头像URI
     */
    fun updateAvatar(uri: Uri?) {
        _uiState.update { it.copy(avatarUri = uri) }
    }

    /**
     * 验证并完成资料设置
     */
    fun completeSetup(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        // 验证
        val validationError = validateProfile(currentState.nickname, currentState.avatarUri)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 生成用户ID
                val userId = UserProfile.generateId()

                // 保存头像文件
                val avatarPath = currentState.avatarUri?.let { uri ->
                    fileManager.saveAvatar(uri, userId).getOrThrow()
                }

                // 创建用户资料
                val profile = UserProfile(
                    id = userId,
                    nickname = currentState.nickname.trim(),
                    avatarPath = avatarPath
                )

                // 保存资料
                profileRepository.saveProfile(profile).getOrThrow()

                _uiState.update { it.copy(isLoading = false) }
                onSuccess()

            } catch (e: Exception) {
                Log.e("profile setup", "completeSetup", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 验证资料
     */
    private fun validateProfile(nickname: String, avatarUri: Uri?): String? {
        return when {
            nickname.isBlank() -> "名字不能为空"
            !UserProfile.isValidName(nickname) -> "名字长度应为2-17个字符"
            avatarUri == null -> "请设置头像"
            else -> null
        }
    }
}