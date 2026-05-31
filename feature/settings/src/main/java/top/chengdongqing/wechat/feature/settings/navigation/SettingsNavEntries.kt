package top.chengdongqing.wechat.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.common.navigation.ContactsKey
import top.chengdongqing.wechat.core.common.navigation.SettingsKey
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.settings.ui.SettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.about.AboutScreen
import top.chengdongqing.wechat.feature.settings.ui.chat.ChatManagementScreen
import top.chengdongqing.wechat.feature.settings.ui.chat.ChatSettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.connection.ConnectionModeSettingScreen
import top.chengdongqing.wechat.feature.settings.ui.display.DarkModeSettingScreen
import top.chengdongqing.wechat.feature.settings.ui.display.DisplaySettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.display.FontScaleSettingScreen
import top.chengdongqing.wechat.feature.settings.ui.display.LanguageSettingScreen
import top.chengdongqing.wechat.feature.settings.ui.more.MoreSettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.more.SystemPermissionSettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.notification.InChatNotificationSettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.notification.NotificationDisplaySettingScreen
import top.chengdongqing.wechat.feature.settings.ui.notification.NotificationSettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.notification.NotificationSoundSettingScreen
import top.chengdongqing.wechat.feature.settings.ui.notification.RingtoneSettingScreen
import top.chengdongqing.wechat.feature.settings.ui.privacy.AddMeMethodSettingScreen
import top.chengdongqing.wechat.feature.settings.ui.privacy.ContactBlacklistScreen
import top.chengdongqing.wechat.feature.settings.ui.privacy.PrivacySettingsScreen

fun EntryProviderScope<NavKey>.settingsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<SettingsKey.Settings> { SettingsScreen(backStack, onBack) }
    entry<SettingsKey.ConnectionMode> { ConnectionModeSettingScreen(onBack) }
    entry<SettingsKey.ChatSettings> { ChatSettingsScreen(onBack) }
    entry<SettingsKey.ChatManagement> { ChatManagementScreen(onBack) }
    entry<SettingsKey.About> { AboutScreen(onBack) }

    notificationNavEntries(backStack, onBack)
    displayNavEntries(backStack, onBack)
    privacyNavEntries(backStack, onBack)
    moreNavEntries(backStack, onBack)
}

private fun EntryProviderScope<NavKey>.notificationNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<SettingsKey.Notification> { NotificationSettingsScreen(backStack, onBack) }
    entry<SettingsKey.NotificationDisplay> { NotificationDisplaySettingScreen(onBack) }
    entry<SettingsKey.InChatNotification> { InChatNotificationSettingsScreen(onBack) }
    entry<SettingsKey.NotificationSound> { NotificationSoundSettingScreen(onBack) }
    entry<SettingsKey.Ringtone> { RingtoneSettingScreen(onBack) }
}

private fun EntryProviderScope<NavKey>.displayNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<SettingsKey.Display> { DisplaySettingsScreen(backStack, onBack) }
    entry<SettingsKey.Theme> { DarkModeSettingScreen(onBack) }
    entry<SettingsKey.Language> { LanguageSettingScreen(onBack) }
    entry<SettingsKey.FontScale> { ChatTheme { FontScaleSettingScreen(onBack) } }
}

private fun EntryProviderScope<NavKey>.privacyNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<SettingsKey.Privacy> { PrivacySettingsScreen(backStack, onBack) }
    entry<SettingsKey.AddMeMethod> { AddMeMethodSettingScreen(onBack) }
    entry<SettingsKey.ContactBlacklist> {
        ContactBlacklistScreen(
            onBack = onBack,
            onNavigateToContactDetail = { backStack.add(ContactsKey.Detail(it)) }
        )
    }
}

private fun EntryProviderScope<NavKey>.moreNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<SettingsKey.More> { MoreSettingsScreen(backStack, onBack) }
    entry<SettingsKey.SystemPermission> { SystemPermissionSettingsScreen(onBack) }
}