package top.chengdongqing.wechat.feature.auth.ui

import android.net.Uri

sealed interface LoginUiIntent {
    // 头像变更
    data class AvatarChanged(val avatarUri: Uri?) : LoginUiIntent

    // 用户名内容变更
    data class UserNameChanged(val userName: String) : LoginUiIntent

    // 点击确定按钮
    data object SubmitForm : LoginUiIntent

    // 清除错误提示
    data object ClearError : LoginUiIntent
}