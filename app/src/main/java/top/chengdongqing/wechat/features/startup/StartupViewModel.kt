package top.chengdongqing.wechat.features.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

sealed class StartupState {
    /** 检查中 */
    object Checking : StartupState()

    /** 需要查看欢迎页和设置资料 */
    object NeedSetup : StartupState()

    /** 已设置资料，准备进入主页 */
    object ReadyForHome : StartupState()
}

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val chatSessionRepository: ChatSessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Checking)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        checkStartupState()
    }

    /**
     * 检查启动状态
     */
    private fun checkStartupState() {
        viewModelScope.launch {
            try {
                chatSessionRepository.preload()
                val hasProfile = profileRepository.hasProfile()

                _state.value = if (hasProfile) {
                    StartupState.ReadyForHome
                } else {
                    StartupState.NeedSetup
                }
            } catch (_: Exception) {
                // 出错时默认进入欢迎页
                _state.value = StartupState.NeedSetup
            }
        }
    }
}