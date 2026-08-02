package top.chengdongqing.wechat.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Cross-feature navigation contract. Destinations are rendered by the app host;
 * no feature may depend on another feature's screen implementation.
 */
@Serializable
sealed interface NavigationKey : NavKey {
    @Serializable data object Splash : NavigationKey
    @Serializable data object Guide : NavigationKey
    @Serializable data object Login : NavigationKey
    @Serializable data object Main : NavigationKey
    @Serializable data class PlainText(val text: String) : NavigationKey
    @Serializable data class WebView(val url: String) : NavigationKey

    @Serializable data class ChatSession(val chatId: String) : NavigationKey
    @Serializable data class ChatInfo(val chatId: String) : NavigationKey
    @Serializable data class FilePreview(val messageId: String) : NavigationKey
    @Serializable
    data class ChatHistory(val payload: String) : NavigationKey
    @Serializable
    data class ChatHistoryFile(
        val path: String,
        val filename: String,
        val mimeType: String,
        val size: Long
    ) : NavigationKey
    @Serializable data class MusicPreview(val messageId: String, val trackName: String) : NavigationKey
    @Serializable
    data class LiveLocation(val chatId: String) : NavigationKey

    // Social navigation contracts.
    @Serializable data class GroupChat(val groupId: String) : NavigationKey
    @Serializable data class GroupInfo(val groupId: String) : NavigationKey
    @Serializable data object GroupList : NavigationKey
    @Serializable data class LiveRoom(
        val groupId: String,
        val liveId: String,
        val isHost: Boolean,
        val hostId: String
    ) : NavigationKey
    @Serializable data object Moments : NavigationKey
    @Serializable data class MomentDetail(val momentId: String) : NavigationKey
    @Serializable data object CreateMoment : NavigationKey
    @Serializable data object ChangeMomentCover : NavigationKey
    @Serializable data object PhotographerCovers : NavigationKey
    @Serializable
    data object IntercomLobby : NavigationKey
    @Serializable
    data class IntercomRoom(val channel: String) : NavigationKey

    @Serializable data object AddFriend : NavigationKey
    @Serializable data object NFCAddFriend : NavigationKey
    @Serializable data object RadarScanAddFriend : NavigationKey
    @Serializable data object PinCodeCreateGroup : NavigationKey
    @Serializable data object NewFriends : NavigationKey
    @Serializable data object ContactTags : NavigationKey
    @Serializable data class EditContactTag(val tagId: String? = null) : NavigationKey
    @Serializable data class ManageContactTags(val contactId: String) : NavigationKey
    @Serializable data class ContactDetail(val contactId: String) : NavigationKey
    @Serializable data class ContactSetting(val contactId: String) : NavigationKey
    @Serializable data class ContactProfile(val contactId: String) : NavigationKey
    @Serializable data class EditContactProfile(val contactId: String) : NavigationKey
    @Serializable data class RequestAddFriend(val contactId: String) : NavigationKey
    @Serializable data class AcceptFriendRequest(val requestId: String) : NavigationKey

    @Serializable data object Profile : NavigationKey
    @Serializable data object QrCode : NavigationKey
    @Serializable data object EditAvatar : NavigationKey
    @Serializable data object EditName : NavigationKey
    @Serializable data object EditId : NavigationKey
    @Serializable data object EditSignature : NavigationKey
    @Serializable data object EditGender : NavigationKey
    @Serializable
    data class Favorites(val targetChatId: String? = null) : NavigationKey
    @Serializable
    data class FavoriteEditor(val favoriteId: String? = null) : NavigationKey
    @Serializable
    data object Services : NavigationKey
    @Serializable
    data object Wallet : NavigationKey
    @Serializable
    data object WalletBalance : NavigationKey
    @Serializable
    data object BankCards : NavigationKey
    @Serializable
    data object PaymentBills : NavigationKey
    @Serializable
    data object PaymentCode : NavigationKey

    @Serializable data object Settings : NavigationKey
    @Serializable
    data object AccountSecuritySettings : NavigationKey
    @Serializable
    data object AppLockSettings : NavigationKey
    @Serializable data object StorageSettings : NavigationKey
    @Serializable data object NotificationSettings : NavigationKey
    @Serializable data object NotificationDisplaySettings : NavigationKey
    @Serializable data object InChatNotificationSettings : NavigationKey
    @Serializable data object NotificationSoundSettings : NavigationKey
    @Serializable data object RingtoneSettings : NavigationKey
    @Serializable data object DisplaySettings : NavigationKey
    @Serializable
    data object AppIconSettings : NavigationKey
    @Serializable data object ThemeSettings : NavigationKey
    @Serializable data object LanguageSettings : NavigationKey
    @Serializable data object FontScaleSettings : NavigationKey
    @Serializable data object PrivacySettings : NavigationKey
    @Serializable data object AddMeMethodSettings : NavigationKey
    @Serializable data object ContactBlacklist : NavigationKey
    @Serializable data object MoreSettings : NavigationKey
    @Serializable data object SystemPermission : NavigationKey
    @Serializable data object ConnectionModeSettings : NavigationKey
    @Serializable data object ChatSettings : NavigationKey
    @Serializable data object ChatManagement : NavigationKey
    @Serializable data object About : NavigationKey
}
