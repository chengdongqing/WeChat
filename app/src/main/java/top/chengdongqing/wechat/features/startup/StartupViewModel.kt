package top.chengdongqing.wechat.features.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * 应用启动状态
 */
sealed class StartupState {
    /** 检查中 */
    object Checking : StartupState()

    /** 需要查看欢迎页和设置资料 */
    object NeedWelcome : StartupState()

    /** 已设置资料，准备进入主页 */
    object ReadyForHome : StartupState()
}

/**
 * 启动状态管理 ViewModel
 *
 * 职责：
 * - 检查用户是否已初始化资料
 * - 决定启动后的导航目标
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Checking)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        checkStartupState()
    }

    /**
     * 检查启动状态
     *
     * 策略：
     * - 如果已有资料 -> ReadyForHome
     * - 如果没有资料 -> NeedWelcome
     */
    private fun checkStartupState() {
        viewModelScope.launch {
            try {
                // 添加最小展示时间，避免闪烁
                delay(100)

                val hasProfile = profileRepository.hasProfile()

                _state.value = if (hasProfile) {
                    StartupState.ReadyForHome
                } else {
                    StartupState.NeedWelcome
                }
            } catch (_: Exception) {
                // 出错时默认进入欢迎页
                _state.value = StartupState.NeedWelcome
            }
        }
    }

    /**
     * 重新检查（用于用户完成设置后）
     */
    fun recheckState() {
        _state.value = StartupState.Checking
        checkStartupState()
    }
}