package top.chengdongqing.wechat.feature.contacts.ui.detail.setting

import top.chengdongqing.wechat.core.navigation.ScreenRoute

sealed interface ContactSettingUiIntent {
    data object Back : ContactSettingUiIntent
    data class NavigateTo(val route: ScreenRoute) : ContactSettingUiIntent

    data object ToggleBlock : ContactSettingUiIntent
    data object ToggleStar : ContactSettingUiIntent
    data object DeleteContact : ContactSettingUiIntent
    data class ShareContact(val targetContactId: String) : ContactSettingUiIntent
}