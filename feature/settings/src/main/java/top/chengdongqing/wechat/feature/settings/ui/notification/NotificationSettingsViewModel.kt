package top.chengdongqing.wechat.feature.settings.ui.notification

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.NotificationSettingsRepository
import top.chengdongqing.wechat.core.notification.NotificationDisplay
import top.chengdongqing.wechat.core.notification.NotificationSound
import top.chengdongqing.wechat.core.playback.RingtoneSound

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: NotificationSettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val msgNotificationEnabled = repository.msgNotificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val callNotificationEnabled = repository.callNotificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val inChatSoundEnabled = repository.inChatSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val inChatVibrationEnabled = repository.inChatVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationDisplay = repository.notificationDisplay
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NotificationDisplay.SenderAndContent
        )

    val notificationSound = repository.notificationSound
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NotificationSound.FollowSystem
        )

    val ringtone = repository.ringtone
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RingtoneSound.Default
        )

    val ringtoneAudibleEnabled = repository.ringtoneAudibleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleMsgNotification(enabled: Boolean) {
        viewModelScope.launch { repository.toggleMsgNotification(enabled) }
    }

    fun toggleCallNotification(enabled: Boolean) {
        viewModelScope.launch { repository.toggleCallNotification(enabled) }
    }

    fun toggleInChatSound(enabled: Boolean) {
        viewModelScope.launch { repository.toggleInChatSound(enabled) }
    }

    fun toggleInChatVibration(enabled: Boolean) {
        viewModelScope.launch { repository.toggleInChatVibration(enabled) }
    }

    fun setNotificationDisplay(display: NotificationDisplay) {
        viewModelScope.launch { repository.setNotificationDisplay(display) }
    }

    fun setNotificationSound(sound: NotificationSound) {
        viewModelScope.launch { repository.setNotificationSound(sound) }
    }

    fun setRingtone(ringtone: RingtoneSound) {
        viewModelScope.launch { repository.setRingtone(ringtone) }
    }

    fun toggleRingtoneAudible(enabled: Boolean) {
        viewModelScope.launch { repository.toggleRingtoneAudible(enabled) }
    }

    private var previewRingtone: Ringtone? = null

    fun previewSound(uri: Uri) {
        previewRingtone?.stop()
        previewRingtone = RingtoneManager.getRingtone(context, uri)
        previewRingtone?.play()
    }

    fun stopPreview() {
        previewRingtone?.stop()
        previewRingtone = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
