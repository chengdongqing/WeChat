package top.chengdongqing.wechat.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.navigation.ScreenRoute
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
import top.chengdongqing.wechat.feature.settings.ui.security.AccountSecurityScreen
import top.chengdongqing.wechat.feature.settings.ui.security.AppLockSettingsScreen
import top.chengdongqing.wechat.feature.settings.ui.storage.StorageSettingsScreen

fun EntryProviderScope<NavKey>.settingsNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.Settings> { SettingsScreen(backStack, onBack) }
    entry<ScreenRoute.AccountSecuritySettings> { AccountSecurityScreen(backStack, onBack) }
    entry<ScreenRoute.AppLockSettings> { AppLockSettingsScreen(onBack) }
    entry<ScreenRoute.ConnectionModeSettings> { ConnectionModeSettingScreen(onBack) }
    entry<ScreenRoute.ChatSettings> { ChatSettingsScreen(onBack) }
    entry<ScreenRoute.ChatManagement> { ChatManagementScreen(onBack) }
    entry<ScreenRoute.About> { AboutScreen(onBack) }
    entry<ScreenRoute.StorageSettings> { StorageSettingsScreen(onBack) }

    notificationNavEntries(backStack, onBack)
    displayNavEntries(backStack, onBack)
    privacyNavEntries(backStack, onBack)
    moreNavEntries(backStack, onBack)
}

private fun EntryProviderScope<NavKey>.notificationNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.NotificationSettings> { NotificationSettingsScreen(backStack, onBack) }
    entry<ScreenRoute.NotificationDisplaySettings> { NotificationDisplaySettingScreen(onBack) }
    entry<ScreenRoute.InChatNotificationSettings> { InChatNotificationSettingsScreen(onBack) }
    entry<ScreenRoute.NotificationSoundSettings> { NotificationSoundSettingScreen(onBack) }
    entry<ScreenRoute.RingtoneSettings> { RingtoneSettingScreen(onBack) }
}

private fun EntryProviderScope<NavKey>.displayNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.DisplaySettings> { DisplaySettingsScreen(backStack, onBack) }
    entry<ScreenRoute.AppIconSettings> { AppIconSettingScreen(onBack) }
    entry<ScreenRoute.ThemeSettings> { DarkModeSettingScreen(onBack) }
    entry<ScreenRoute.LanguageSettings> { LanguageSettingScreen(onBack) }
    entry<ScreenRoute.FontScaleSettings> { FontScaleSettingScreen(onBack) }
}

private fun EntryProviderScope<NavKey>.privacyNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.PrivacySettings> { PrivacySettingsScreen(backStack, onBack) }
    entry<ScreenRoute.AddMeMethodSettings> { AddMeMethodSettingScreen(onBack) }
    entry<ScreenRoute.ContactBlacklist> {
        ContactBlacklistScreen(
            onBack = onBack,
            onContactDetail = { backStack.add(ScreenRoute.ContactDetail(it)) }
        )
    }
}

private fun EntryProviderScope<NavKey>.moreNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.MoreSettings> { MoreSettingsScreen(backStack, onBack) }
    entry<ScreenRoute.SystemPermission> { SystemPermissionSettingsScreen(onBack) }
}
