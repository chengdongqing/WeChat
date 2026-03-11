package top.chengdongqing.wechat.features.settings.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.settings.domain.model.NotificationDisplay
import top.chengdongqing.wechat.features.settings.domain.model.NotificationSound
import top.chengdongqing.wechat.features.settings.domain.repository.NotificationSettingsRepository

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: NotificationSettingsRepository
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

    fun toggleRingtoneAudible(enabled: Boolean) {
        viewModelScope.launch { repository.toggleRingtoneAudible(enabled) }
    }
}