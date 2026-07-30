package top.chengdongqing.wechat.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.settings.ui.SettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.about.AboutScreen
import top.chengdongqing.wechat.feature.settings.ui.chat.ChatManagementScreen
import top.chengdongqing.wechat.feature.settings.ui.chat.ChatSettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.connection.ConnectionModeSettingScreen
import top.chengdongqing.wechat.feature.settings.ui.display.AppIconSettingScreen
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
import top.chengdongqing.wechat.feature.settings.ui.storage.StorageSettingsScreen

fun EntryProviderScope<NavKey>.settingsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.Settings> { SettingsScreen(backStack, onBack) }
    entry<NavigationKey.ConnectionModeSettings> { ConnectionModeSettingScreen(onBack) }
    entry<NavigationKey.ChatSettings> { ChatSettingsScreen(onBack) }
    entry<NavigationKey.ChatManagement> { ChatManagementScreen(onBack) }
    entry<NavigationKey.About> { AboutScreen(onBack) }
    entry<NavigationKey.StorageSettings> { StorageSettingsScreen(onBack) }

    notificationNavEntries(backStack, onBack)
    displayNavEntries(backStack, onBack)
    privacyNavEntries(backStack, onBack)
    moreNavEntries(backStack, onBack)
}

private fun EntryProviderScope<NavKey>.notificationNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.NotificationSettings> { NotificationSettingsScreen(backStack, onBack) }
    entry<NavigationKey.NotificationDisplaySettings> { NotificationDisplaySettingScreen(onBack) }
    entry<NavigationKey.InChatNotificationSettings> { InChatNotificationSettingsScreen(onBack) }
    entry<NavigationKey.NotificationSoundSettings> { NotificationSoundSettingScreen(onBack) }
    entry<NavigationKey.RingtoneSettings> { RingtoneSettingScreen(onBack) }
}

private fun EntryProviderScope<NavKey>.displayNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.DisplaySettings> { DisplaySettingsScreen(backStack, onBack) }
    entry<NavigationKey.AppIconSettings> { AppIconSettingScreen(onBack) }
    entry<NavigationKey.ThemeSettings> { DarkModeSettingScreen(onBack) }
    entry<NavigationKey.LanguageSettings> { LanguageSettingScreen(onBack) }
    entry<NavigationKey.FontScaleSettings> { FontScaleSettingScreen(onBack) }
}

private fun EntryProviderScope<NavKey>.privacyNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.PrivacySettings> { PrivacySettingsScreen(backStack, onBack) }
    entry<NavigationKey.AddMeMethodSettings> { AddMeMethodSettingScreen(onBack) }
    entry<NavigationKey.ContactBlacklist> {
        ContactBlacklistScreen(
            onBack = onBack,
            onNavigateToContactDetail = { backStack.add(NavigationKey.ContactDetail(it)) }
        )
    }
}

private fun EntryProviderScope<NavKey>.moreNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.MoreSettings> { MoreSettingsScreen(backStack, onBack) }
    entry<NavigationKey.SystemPermission> { SystemPermissionSettingsScreen(onBack) }
}
