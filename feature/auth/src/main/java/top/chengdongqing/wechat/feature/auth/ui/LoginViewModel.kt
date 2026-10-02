package top.chengdongqing.wechat.feature.auth.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.UserProfile
import top.chengdongqing.wechat.core.network.security.KeyStoreManager
import top.chengdongqing.wechat.feature.auth.R
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val privateFileManager: PrivateFileManager,
    private val keyStoreManager: KeyStoreManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onIntent(intent: LoginUiIntent) {
        when (intent) {
            is LoginUiIntent.AvatarChanged -> {
                _uiState.update {
                    it.copy(avatarUri = intent.avatarUri)
                }
            }

            is LoginUiIntent.UserNameChanged -> {
                _uiState.update {
                    it.copy(userName = intent.userName)
                }
            }

            is LoginUiIntent.SubmitForm -> {
                handleLogin()
            }

            is LoginUiIntent.ClearError -> {
                _uiState.update {
                    it.copy(errorMessage = null)
                }
            }
        }
    }

    /**
     * 验证并完成资料设置
     */
    fun handleLogin() {
        val current = _uiState.value

        viewModelScope.launch {
            runCatching {
                // 验证表单
                validateForm()

                // 显示loading
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        errorMessage = null
                    )
                }

                // 生成用户ID
                val userId = UserProfile.generateId()
                // 生成密钥
                val publicKey = keyStoreManager.generateKeyPair()

                // 保存头像文件
                val avatarPath = current.avatarUri?.let { uri ->
                    privateFileManager.saveAvatar(userId, uri).getOrThrow()
                }

                // 创建用户资料
                val profile = UserProfile(
                    id = userId,
                    nickname = current.userName.trim(),
                    avatarPath = avatarPath,
                    publicKey = publicKey
                )

                // 保存资料
                profileRepository.saveProfile(profile)

                // 发送跳转到首页的信号
                _uiEvent.send(LoginUiEvent.NavigateToHome)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(errorMessage = e.message)
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
    private fun validateForm() {
        val form = _uiState.value

        if (form.avatarUri == null) {
            error(context.getString(R.string.error_avatar_required))
        }
        if (form.userName.isBlank()) {
            error(context.getString(R.string.error_username_required))
        }
        if (!UserProfile.isValidName(form.userName)) {
            error(context.getString(R.string.error_username_length))
        }
    }
}
