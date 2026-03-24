package top.chengdongqing.wechat.core.common.navigation

import top.chengdongqing.wechat.core.common.util.encode

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")

    object PlainText : Screen("plain_text/{text}") {
        const val ARG_TEXT = "text"
        fun createRoute(text: String) = "plain_text/${text.encode()}"
    }

    data object WebView : Screen("webview/{url}") {
        const val ARG_URL = "url"
        fun createRoute(url: String) = "webview/${url.encode()}"
    }
}

sealed class ChatRoute(val route: String) {
    object ChatSession : ChatRoute("chats/{chatId}") {
        const val ARG_CHAT_ID = "chatId"
        fun createRoute(chatId: String) = "chats/${chatId}"
    }

    object ChatInfo : ChatRoute("chats/{chatId}/info") {
        const val ARG_CHAT_ID = "chatId"
        fun createRoute(chatId: String) = "chats/${chatId}/info"
    }

    object FilePreview : ChatRoute("chats/{messageId}/preview/file") {
        const val ARG_MESSAGE_ID = "messageId"
        fun createRoute(messageId: String) = "chats/${messageId}/preview/file"
    }

    object MusicPreview : ChatRoute("chats/{messageId}/preview/music/{trackName}") {
        const val ARG_MESSAGE_ID = "messageId"
        const val ARG_TRACK_NAME = "trackName"
        fun createRoute(messageId: String, trackName: String) =
            "chats/${messageId}/preview/music/${trackName}"
    }
}

sealed class ContactsRoute(val route: String) {
    object AddContact : ContactsRoute("contacts/add")
    object NFC : ContactsRoute("contacts/add/nfc")
    object RadarScan : ContactsRoute("contacts/add/radar_scan")
    object PinCodeGroup : ContactsRoute("contacts/add/pin_code_group")

    object Detail : ContactsRoute("contacts/{contactId}") {
        const val ARG_CONTACT_ID = "contactId"
        fun createRoute(contactId: String) = "contacts/${contactId}"
    }

    object Setting : ContactsRoute("contacts/{contactId}/setting") {
        const val ARG_CONTACT_ID = "contactId"
        fun createRoute(contactId: String) = "contacts/${contactId}/setting"
    }

    object Profile : ContactsRoute("contacts/{contactId}/profile") {
        const val ARG_CONTACT_ID = "contactId"
        fun createRoute(contactId: String) = "contacts/${contactId}/profile"
    }

    object ProfileEdit : ContactsRoute("contacts/{contactId}/profile/edit") {
        const val ARG_CONTACT_ID = "contactId"
        fun createRoute(contactId: String) = "contacts/${contactId}/profile/edit"
    }

    object RequestAdd : ContactsRoute("contacts/{contactId}/request_add") {
        const val ARG_CONTACT_ID = "contactId"
        fun createRoute(contactId: String) = "contacts/${contactId}/request_add"
    }

    object AcceptVerify : ContactsRoute("contacts/{requestId}/accept_verify") {
        const val ARG_REQUEST_ID = "requestId"
        fun createRoute(requestId: String) = "contacts/${requestId}/accept_verify"
    }

    object NewFriends : ContactsRoute("contacts/new_friends")
}

object MeRoute {
    private const val ROOT = "me"
    const val PROFILE = "$ROOT/profile"
    const val QR_CODE = "$ROOT/qrcode"

    object Edit {
        private const val EDIT_ROOT = "$PROFILE/edit"
        const val AVATAR = "$EDIT_ROOT/avatar"
        const val NAME = "$EDIT_ROOT/name"
        const val ID = "$EDIT_ROOT/id"
        const val SIGNATURE = "$EDIT_ROOT/signature"
        const val GENDER = "$EDIT_ROOT/gender"
    }
}

sealed class SettingsRoute(val route: String) {
    object Settings : SettingsRoute("settings")
    object NotificationSettings : SettingsRoute("settings/notification")
    object NotificationDisplaySetting : SettingsRoute("settings/notification/display")
    object InChatNotificationSetting : SettingsRoute("settings/notification/in_chat")
    object NotificationSoundSetting : SettingsRoute("settings/notification/sound")
    object RingtoneSetting : SettingsRoute("settings/notification/ringtone")
    object DisplaySettings : SettingsRoute("settings/display")
    object ThemeSetting : SettingsRoute("settings/display/theme")
    object LanguageSetting : SettingsRoute("settings/display/language")
    object FontScaleSetting : SettingsRoute("settings/display/font_scale")
    object PrivacySettings : SettingsRoute("settings/privacy")
    object AddMeMethodSetting : SettingsRoute("settings/privacy/add_me_method")
    object ContactBlacklist : SettingsRoute("settings/privacy/contact_blacklist")
    object MoreSettings : SettingsRoute("settings/more")
    object SystemPermissionSettings : SettingsRoute("settings/more/system_permission")
    object ConnectionModeSettings : SettingsRoute("settings/chat/connection_mode")
    object ChatSettings : SettingsRoute("settings/chat")
    object ChatManagement : SettingsRoute("settings/chat/management")
    object About : SettingsRoute("settings/about")
}
