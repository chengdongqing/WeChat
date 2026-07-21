package top.chengdongqing.wechat.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import javax.inject.Inject

enum class LoginState {
    Checking,
    NeedLogin,
    ReadyForHome
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val chatSessionRepository: ChatSessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState.Checking)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        checkStartupState()
    }

    /**
     * 检查启动状态
     */
    private fun checkStartupState() {
        viewModelScope.launch {
            runCatching {
                // 数据库预热
                chatSessionRepository.preload()
                // 判断是否有个人资料
                val hasSetup = profileRepository.getProfile() != null

                _state.value = if (hasSetup) {
                    LoginState.ReadyForHome
                } else {
                    LoginState.NeedLogin
                }
            }.onFailure {
                _state.value = LoginState.NeedLogin
            }
        }
    }
}