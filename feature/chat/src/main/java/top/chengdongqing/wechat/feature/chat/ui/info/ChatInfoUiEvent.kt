package top.chengdongqing.wechat.feature.chat.ui.info

sealed interface ChatInfoUiEvent {
    data object OnTemporaryChatEnd : ChatInfoUiEvent
}