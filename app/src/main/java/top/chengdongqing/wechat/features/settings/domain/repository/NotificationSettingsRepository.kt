package top.chengdongqing.wechat.features.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.settings.domain.model.NotificationDisplay
import top.chengdongqing.wechat.features.settings.domain.model.NotificationSound
import top.chengdongqing.wechat.features.settings.domain.model.RingtoneSound

interface NotificationSettingsRepository {

    /**
     * 消息通知开关
     */
    val msgNotificationEnabled: Flow<Boolean>

    /**
     * 来电通知开关
     */
    val callNotificationEnabled: Flow<Boolean>

    /**
     * 聊天中收到消息时播放声音
     */
    val inChatSoundEnabled: Flow<Boolean>

    /**
     * 聊天中收到消息时震动
     */
    val inChatVibrationEnabled: Flow<Boolean>

    /**
     * 通知栏显示方式
     */
    val notificationDisplay: Flow<NotificationDisplay>

    /**
     * 消息通知提示音
     */
    val notificationSound: Flow<NotificationSound>

    /**
     * 来电铃声
     */
    val ringtone: Flow<RingtoneSound>

    /**
     * 来电时朋友可听见铃声
     */
    val ringtoneAudibleEnabled: Flow<Boolean>

    suspend fun toggleMsgNotification(enabled: Boolean)
    suspend fun toggleCallNotification(enabled: Boolean)
    suspend fun toggleInChatSound(enabled: Boolean)
    suspend fun toggleInChatVibration(enabled: Boolean)
    suspend fun setNotificationDisplay(display: NotificationDisplay)
    suspend fun setNotificationSound(sound: NotificationSound)
    suspend fun setRingtone(ringtone: RingtoneSound)
    suspend fun toggleRingtoneAudible(enabled: Boolean)

    /**
     * 退出登录时清除所有设置
     */
    suspend fun clearAll()
}