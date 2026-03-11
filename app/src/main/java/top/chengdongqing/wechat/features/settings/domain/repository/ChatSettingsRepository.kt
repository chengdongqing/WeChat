package top.chengdongqing.wechat.features.settings.domain.repository

import kotlinx.coroutines.flow.Flow

interface ChatSettingsRepository {

    /**
     * 是否使用扬声器播放语音
     */
    val speakerEnabled: Flow<Boolean>

    /**
     * 使用独立的发送按钮
     */
    val sendButtonEnabled: Flow<Boolean>

    /**
     * 启用端到端加密
     */
    val e2eEnabled: Flow<Boolean>

    suspend fun toggleSpeaker(enabled: Boolean)

    suspend fun toggleSendButton(enabled: Boolean)

    suspend fun toggleE2e(enabled: Boolean)
}