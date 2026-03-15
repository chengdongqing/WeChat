package top.chengdongqing.wechat.features.me.ui.setup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.data.security.LocalIdentity
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
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
    private val privateFileManager: PrivateFileManager,
    private val localIdentity: LocalIdentity,
    @param:ApplicationContext private val context: Context
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
                // 生成密钥
                val publicKey = localIdentity.generateKeyPair()

                // 保存头像文件
                val avatarPath = currentState.avatarUri?.let { uri ->
                    privateFileManager.saveAvatar(uri, userId).getOrThrow()
                }

                // 创建用户资料
                val profile = UserProfile(
                    id = userId,
                    nickname = currentState.nickname.trim(),
                    avatarPath = avatarPath,
                    publicKey = publicKey
                )

                // 保存资料
                profileRepository.saveProfile(profile)

                _uiState.update { it.copy(isLoading = false) }
                onSuccess()

            } catch (e: Exception) {
                Log.e("profile setup", "completeSetup", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "${context.getString(R.string.msg_save_failed)}: ${e.message}"
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
            nickname.isBlank() -> context.getString(R.string.setup_error_name_empty)
            !UserProfile.isValidName(nickname) -> context.getString(R.string.setup_error_name_length)
            avatarUri == null -> context.getString(R.string.setup_error_avatar_required)
            else -> null
        }
    }
}