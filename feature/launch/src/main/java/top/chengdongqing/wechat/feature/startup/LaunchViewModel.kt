package top.chengdongqing.wechat.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val chatSessionRepository: ChatSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LaunchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkLoginState()
    }

    private fun checkLoginState() {
        viewModelScope.launch {
            runCatching {
                // 数据库预热
                chatSessionRepository.preload()
                // 判断是否有个人资料
                val hasProfile = profileRepository.getProfile() != null

                _uiState.update {
                    it.copy(
                        loginState = if (hasProfile) {
                            LoginState.HasLogin
                        } else {
                            LoginState.NeedLogin
                        }
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(loginState = LoginState.NeedLogin)
                }
            }
        }
    }
}