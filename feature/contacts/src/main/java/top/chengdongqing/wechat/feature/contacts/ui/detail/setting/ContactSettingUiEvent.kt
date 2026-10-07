package top.chengdongqing.wechat.feature.contacts.ui.detail.setting

sealed interface ContactSettingUiEvent {
    data object OnContactDeleted : ContactSettingUiEvent
}