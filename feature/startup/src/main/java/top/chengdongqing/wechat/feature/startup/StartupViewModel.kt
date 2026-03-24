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

enum class StartupState {
    Checking, // 检查中
    NeedSetup, // 需要设置资料
    ReadyForHome // 已设置资料，准备进入主页
}

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val chatSessionRepository: ChatSessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StartupState.Checking)
    val state: StateFlow<StartupState> = _state.asStateFlow()

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
                    StartupState.ReadyForHome
                } else {
                    StartupState.NeedSetup
                }
            }.onFailure {
                // 出错时默认进入欢迎页
                _state.value = StartupState.NeedSetup
            }
        }
    }
}