package top.chengdongqing.wechat.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 页面路由定义
 */
@Serializable
sealed interface ScreenRoute : NavKey {
    @Serializable
    data object Launch : ScreenRoute

    @Serializable
    data object Guide : ScreenRoute

    @Serializable
    data object Login : ScreenRoute

    @Serializable
    data object Home : ScreenRoute

    @Serializable
    data class PlainText(val text: String) : ScreenRoute

    @Serializable
    data class WebView(val url: String) : ScreenRoute

    @Serializable
    data class Chat(val chatId: String) : ScreenRoute

    @Serializable
    data class ChatInfo(val chatId: String) : ScreenRoute

    @Serializable
    data class FilePreview(val messageId: String) : ScreenRoute

    @Serializable
    data class ChatHistory(val payload: String) : ScreenRoute

    @Serializable
    data class ChatHistoryFile(
        val path: String,
        val filename: String,
        val mimeType: String,
        val size: Long
    ) : ScreenRoute

    @Serializable
    data class MusicPreview(val messageId: String, val trackName: String) : ScreenRoute

    @Serializable
    data class LiveLocation(val chatId: String) : ScreenRoute

    @Serializable
    data class GroupChat(val groupId: String) : ScreenRoute

    @Serializable
    data class GroupInfo(val groupId: String) : ScreenRoute

    @Serializable
    data object GroupList : ScreenRoute

    @Serializable
    data class LiveRoom(
        val groupId: String,
        val liveId: String,
        val isHost: Boolean,
        val hostId: String
    ) : ScreenRoute

    @Serializable
    data object Moments : ScreenRoute

    @Serializable
    data object PostMoment : ScreenRoute

    @Serializable
    data object ChangeMomentCover : ScreenRoute

    @Serializable
    data object PhotographerCovers : ScreenRoute

    @Serializable
    data object IntercomLobby : ScreenRoute

    @Serializable
    data class IntercomRoom(val channel: String) : ScreenRoute

    @Serializable
    data object AddFriend : ScreenRoute

    @Serializable
    data object NFCAddFriend : ScreenRoute

    @Serializable
    data object RadarScanAddFriend : ScreenRoute

    @Serializable
    data object PinCodeCreateGroup : ScreenRoute

    @Serializable
    data object NewFriends : ScreenRoute

    @Serializable
    data object ContactTags : ScreenRoute

    @Serializable
    data class EditContactTag(val tagId: String? = null) : ScreenRoute

    @Serializable
    data class ManageContactTags(val contactId: String) : ScreenRoute

    @Serializable
    data class ContactDetail(val contactId: String) : ScreenRoute

    @Serializable
    data class ContactSetting(val contactId: String) : ScreenRoute

    @Serializable
    data class ContactProfile(val contactId: String) : ScreenRoute

    @Serializable
    data class EditContactProfile(val contactId: String) : ScreenRoute

    @Serializable
    data class RequestAddFriend(val contactId: String) : ScreenRoute

    @Serializable
    data class AcceptFriendRequest(val requestId: String) : ScreenRoute

    @Serializable
    data object Profile : ScreenRoute

    @Serializable
    data object QrCode : ScreenRoute

    @Serializable
    data object EditAvatar : ScreenRoute

    @Serializable
    data object EditName : ScreenRoute

    @Serializable
    data object EditId : ScreenRoute

    @Serializable
    data object EditSignature : ScreenRoute

    @Serializable
    data object EditGender : ScreenRoute

    @Serializable
    data class Favorites(val targetChatId: String? = null) : ScreenRoute

    @Serializable
    data class FavoriteEditor(val favoriteId: String? = null) : ScreenRoute

    @Serializable
    data object Services : ScreenRoute

    @Serializable
    data object Wallet : ScreenRoute

    @Serializable
    data object WalletBalance : ScreenRoute

    @Serializable
    data object BankCards : ScreenRoute

    @Serializable
    data object PaymentBills : ScreenRoute

    @Serializable
    data object Money : ScreenRoute

    @Serializable
    data object Settings : ScreenRoute

    @Serializable
    data object AccountSecuritySettings : ScreenRoute

    @Serializable
    data object AppLockSettings : ScreenRoute

    @Serializable
    data object StorageSettings : ScreenRoute

    @Serializable
    data object NotificationSettings : ScreenRoute

    @Serializable
    data object NotificationDisplaySettings : ScreenRoute

    @Serializable
    data object InChatNotificationSettings : ScreenRoute

    @Serializable
    data object NotificationSoundSettings : ScreenRoute

    @Serializable
    data object RingtoneSettings : ScreenRoute

    @Serializable
    data object DisplaySettings : ScreenRoute

    @Serializable
    data object AppIconSettings : ScreenRoute

    @Serializable
    data object ThemeSettings : ScreenRoute

    @Serializable
    data object LanguageSettings : ScreenRoute

    @Serializable
    data object FontScaleSettings : ScreenRoute

    @Serializable
    data object PrivacySettings : ScreenRoute

    @Serializable
    data object AddMeMethodSettings : ScreenRoute

    @Serializable
    data object ContactBlacklist : ScreenRoute

    @Serializable
    data object MoreSettings : ScreenRoute

    @Serializable
    data object SystemPermission : ScreenRoute

    @Serializable
    data object ConnectionModeSettings : ScreenRoute

    @Serializable
    data object ChatSettings : ScreenRoute

    @Serializable
    data object ChatManagement : ScreenRoute

    @Serializable
    data object About : ScreenRoute
}
