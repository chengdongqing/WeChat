package top.chengdongqing.wechat.feature.chat.ui.info

import android.net.Uri

sealed interface ChatInfoUiIntent {
    data object ToggleMuted : ChatInfoUiIntent
    data object TogglePinned : ChatInfoUiIntent
    data object ToggleBottomed : ChatInfoUiIntent
    data object EndTemporaryChat : ChatInfoUiIntent
    data class UpdateBackground(val uri: Uri?) : ChatInfoUiIntent
    data object ClearChatHistory : ChatInfoUiIntent
    data class ImportLocalAiModel(val uri: Uri) : ChatInfoUiIntent
    data object CancelModelLoading : ChatInfoUiIntent
    data object UnloadModel : ChatInfoUiIntent
}