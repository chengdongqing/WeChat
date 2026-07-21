package top.chengdongqing.wechat.core.common.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavigationKey : NavKey {
    @Serializable
    data object Splash : NavigationKey

    @Serializable
    data object Guide : NavigationKey

    @Serializable
    data object Login : NavigationKey

    @Serializable
    data object Home : NavigationKey

    @Serializable
    data class PlainText(val text: String) : NavigationKey

    @Serializable
    data class WebView(val url: String) : NavigationKey

    @Serializable
    data class ChatSession(val chatId: String) : NavigationKey

    @Serializable
    data class ChatInfo(val chatId: String) : NavigationKey

    @Serializable
    data class FilePreview(val messageId: String) : NavigationKey

    @Serializable
    data class MusicPreview(
        val messageId: String,
        val trackName: String
    ) : NavigationKey

    @Serializable
    data object AddFriend : NavigationKey

    @Serializable
    data object NFCAddFriend : NavigationKey

    @Serializable
    data object RadarScanAddFriend : NavigationKey

    @Serializable
    data object PinCodeCreateGroup : NavigationKey

    @Serializable
    data object NewFriends : NavigationKey

    @Serializable
    data class ContactDetail(val contactId: String) : NavigationKey

    @Serializable
    data class ContactSetting(val contactId: String) : NavigationKey

    @Serializable
    data class ContactProfile(val contactId: String) : NavigationKey

    @Serializable
    data class EditContactProfile(val contactId: String) : NavigationKey

    @Serializable
    data class RequestAddFriend(val contactId: String) : NavigationKey

    @Serializable
    data class AcceptFriendRequest(val requestId: String) : NavigationKey

    @Serializable
    data object Profile : NavigationKey

    @Serializable
    data object QrCode : NavigationKey

    @Serializable
    data object EditAvatar : NavigationKey

    @Serializable
    data object EditName : NavigationKey

    @Serializable
    data object EditId : NavigationKey

    @Serializable
    data object EditSignature : NavigationKey

    @Serializable
    data object EditGender : NavigationKey

    @Serializable
    data object Settings : NavigationKey

    @Serializable
    data object NotificationSettings : NavigationKey

    @Serializable
    data object NotificationDisplaySettings : NavigationKey

    @Serializable
    data object InChatNotificationSettings : NavigationKey

    @Serializable
    data object NotificationSoundSettings : NavigationKey

    @Serializable
    data object RingtoneSettings : NavigationKey

    @Serializable
    data object DisplaySettings : NavigationKey

    @Serializable
    data object ThemeSettings : NavigationKey

    @Serializable
    data object LanguageSettings : NavigationKey

    @Serializable
    data object FontScaleSettings : NavigationKey

    @Serializable
    data object PrivacySettings : NavigationKey

    @Serializable
    data object AddMeMethodSettings : NavigationKey

    @Serializable
    data object ContactBlacklist : NavigationKey

    @Serializable
    data object MoreSettings : NavigationKey

    @Serializable
    data object SystemPermission : NavigationKey

    @Serializable
    data object ConnectionModeSettings : NavigationKey

    @Serializable
    data object ChatSettings : NavigationKey

    @Serializable
    data object ChatManagement : NavigationKey

    @Serializable
    data object About : NavigationKey
}