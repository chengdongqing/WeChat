package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow

interface ChatSettingsRepository {
    val speakerEnabled: Flow<Boolean>
    val sendButtonEnabled: Flow<Boolean>
    val e2eEnabled: Flow<Boolean>
    val chatBackground: Flow<String?>
    suspend fun toggleSpeaker(enabled: Boolean)
    suspend fun toggleSendButton(enabled: Boolean)
    suspend fun toggleE2e(enabled: Boolean)
    suspend fun setChatBackground(path: String?)
}
