package top.chengdongqing.wechat.feature.chat.ui.list

sealed interface ChatListUiIntent {
    data class MarkAsRead(val chatId: String) : ChatListUiIntent
    data class MarkAsUnread(val chatId: String) : ChatListUiIntent
    data class PinToTop(val chatId: String) : ChatListUiIntent
    data class RemoveFromTop(val chatId: String) : ChatListUiIntent
    data class HideChat(val chatId: String) : ChatListUiIntent
    data class DeleteChat(val chatId: String) : ChatListUiIntent

    data object MarkEnterScreen : ChatListUiIntent
    data object MarkLeaveScreen : ChatListUiIntent
}