package top.chengdongqing.wechat.feature.startup

import androidx.compose.runtime.Immutable

enum class LoginState {
    Checking,
    NeedLogin,
    HasLogin
}

@Immutable
data class LaunchUiState(
    val loginState: LoginState = LoginState.Checking
)