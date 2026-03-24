package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.common.media.RingtoneSound
import top.chengdongqing.wechat.core.common.notification.NotificationDisplay
import top.chengdongqing.wechat.core.common.notification.NotificationSound

interface NotificationSettingsRepository {
    val msgNotificationEnabled: Flow<Boolean>
    val callNotificationEnabled: Flow<Boolean>
    val inChatSoundEnabled: Flow<Boolean>
    val inChatVibrationEnabled: Flow<Boolean>
    val notificationDisplay: Flow<NotificationDisplay>
    val notificationSound: Flow<NotificationSound>
    val ringtone: Flow<RingtoneSound>
    val ringtoneAudibleEnabled: Flow<Boolean>
    suspend fun toggleMsgNotification(enabled: Boolean)
    suspend fun toggleCallNotification(enabled: Boolean)
    suspend fun toggleInChatSound(enabled: Boolean)
    suspend fun toggleInChatVibration(enabled: Boolean)
    suspend fun setNotificationDisplay(display: NotificationDisplay)
    suspend fun setNotificationSound(sound: NotificationSound)
    suspend fun setRingtone(ringtone: RingtoneSound)
    suspend fun toggleRingtoneAudible(enabled: Boolean)
}
