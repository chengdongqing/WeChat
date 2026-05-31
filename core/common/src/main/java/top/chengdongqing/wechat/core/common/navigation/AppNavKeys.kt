package top.chengdongqing.wechat.core.common.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppNavKey : NavKey

@Serializable
sealed interface CommonKey : AppNavKey {
    @Serializable
    data object Splash : CommonKey

    @Serializable
    data object Welcome : CommonKey

    @Serializable
    data object Setup : CommonKey

    @Serializable
    data object Home : CommonKey

    @Serializable
    data class PlainText(val text: String) : CommonKey

    @Serializable
    data class WebView(val url: String) : CommonKey
}

@Serializable
sealed interface ChatKey : AppNavKey {
    @Serializable
    data class ChatSession(val chatId: String) : ChatKey

    @Serializable
    data class ChatInfo(val chatId: String) : ChatKey

    @Serializable
    data class FilePreview(val messageId: String) : ChatKey

    @Serializable
    data class MusicPreview(
        val messageId: String,
        val trackName: String
    ) : ChatKey
}

@Serializable
sealed interface ContactsKey : AppNavKey {
    @Serializable
    data object AddContact : ContactsKey

    @Serializable
    data object NFC : ContactsKey

    @Serializable
    data object RadarScan : ContactsKey

    @Serializable
    data object PinCodeGroup : ContactsKey

    @Serializable
    data object NewFriends : ContactsKey

    @Serializable
    data class Detail(val contactId: String) : ContactsKey

    @Serializable
    data class Setting(val contactId: String) : ContactsKey

    @Serializable
    data class Profile(val contactId: String) : ContactsKey

    @Serializable
    data class EditProfile(val contactId: String) : ContactsKey

    @Serializable
    data class RequestAdd(val contactId: String) : ContactsKey

    @Serializable
    data class AcceptVerify(val requestId: String) : ContactsKey
}

@Serializable
sealed interface MeKey : AppNavKey {
    @Serializable
    data object Profile : MeKey

    @Serializable
    data object QrCode : MeKey

    @Serializable
    data object EditAvatar : MeKey

    @Serializable
    data object EditName : MeKey

    @Serializable
    data object EditId : MeKey

    @Serializable
    data object EditSignature : MeKey

    @Serializable
    data object EditGender : MeKey
}

@Serializable
sealed interface SettingsKey : AppNavKey {
    @Serializable
    data object Settings : SettingsKey

    @Serializable
    data object Notification : SettingsKey

    @Serializable
    data object NotificationDisplay : SettingsKey

    @Serializable
    data object InChatNotification : SettingsKey

    @Serializable
    data object NotificationSound : SettingsKey

    @Serializable
    data object Ringtone : SettingsKey

    @Serializable
    data object Display : SettingsKey

    @Serializable
    data object Theme : SettingsKey

    @Serializable
    data object Language : SettingsKey

    @Serializable
    data object FontScale : SettingsKey

    @Serializable
    data object Privacy : SettingsKey

    @Serializable
    data object AddMeMethod : SettingsKey

    @Serializable
    data object ContactBlacklist : SettingsKey

    @Serializable
    data object More : SettingsKey

    @Serializable
    data object SystemPermission : SettingsKey

    @Serializable
    data object ConnectionMode : SettingsKey

    @Serializable
    data object ChatSettings : SettingsKey

    @Serializable
    data object ChatManagement : SettingsKey

    @Serializable
    data object About : SettingsKey
}