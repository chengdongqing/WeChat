package top.chengdongqing.wechat.feature.auth.ui

sealed interface LoginUiEvent {
    data object NavigateToHome : LoginUiEvent
}