package top.chengdongqing.wechat.features.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import top.chengdongqing.wechat.features.contacts.navigation.ContactsRoute
import top.chengdongqing.wechat.features.settings.ui.SettingsScreen
import top.chengdongqing.wechat.features.settings.ui.display.DarkModeSettingScreen
import top.chengdongqing.wechat.features.settings.ui.display.DisplaySettingsScreen
import top.chengdongqing.wechat.features.settings.ui.display.FontSizeSettingScreen
import top.chengdongqing.wechat.features.settings.ui.display.LanguageSettingScreen
import top.chengdongqing.wechat.features.settings.ui.notification.InChatNotificationSettingsScreen
import top.chengdongqing.wechat.features.settings.ui.notification.NotificationDisplaySettingScreen
import top.chengdongqing.wechat.features.settings.ui.notification.NotificationSettingsScreen
import top.chengdongqing.wechat.features.settings.ui.notification.NotificationSoundSettingScreen
import top.chengdongqing.wechat.features.settings.ui.privacy.AddMeMethodSettingScreen
import top.chengdongqing.wechat.features.settings.ui.privacy.ContactBlacklistScreen
import top.chengdongqing.wechat.features.settings.ui.privacy.PrivacySettingsScreen

sealed class SettingsRoute(val route: String) {
    object Settings : SettingsRoute("settings")

    object NotificationSettings : SettingsRoute("settings/notification")
    object NotificationDisplaySetting : SettingsRoute("settings/notification/display")
    object InChatNotificationSetting : SettingsRoute("settings/notification/in_chat")
    object NotificationSoundSetting : SettingsRoute("settings/notification/sound")

    object DisplaySettings : SettingsRoute("settings/display")
    object ThemeSetting : SettingsRoute("settings/display/theme")
    object LanguageSetting : SettingsRoute("settings/display/language")
    object FontSizeSetting : SettingsRoute("settings/display/fontSize")

    object PrivacySettings : SettingsRoute("settings/privacy")
    object AddMeMethodSetting : SettingsRoute("settings/privacy/add_me_method")
    object ContactBlacklist : SettingsRoute("settings/privacy/contact_blacklist")
}

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    onBack: () -> Unit
) {
    composable(SettingsRoute.Settings.route) {
        SettingsScreen(navController, onBack)
    }

    notificationNavGraph(navController, onBack)
    displayNavGraph(navController, onBack)
    privacyNavGraph(navController, onBack)
}

private fun NavGraphBuilder.privacyNavGraph(
    navController: NavHostController,
    onBack: () -> Unit
) {
    composable(SettingsRoute.PrivacySettings.route) {
        PrivacySettingsScreen(navController, onBack)
    }
    composable(SettingsRoute.AddMeMethodSetting.route) {
        AddMeMethodSettingScreen(onBack)
    }
    composable(SettingsRoute.ContactBlacklist.route) {
        ContactBlacklistScreen(onBack) { id ->
            navController.navigate(ContactsRoute.Detail.createRoute(id))
        }
    }
}

private fun NavGraphBuilder.displayNavGraph(
    navController: NavHostController,
    onBack: () -> Unit
) {
    composable(SettingsRoute.DisplaySettings.route) {
        DisplaySettingsScreen(navController, onBack)
    }
    composable(SettingsRoute.ThemeSetting.route) {
        DarkModeSettingScreen(onBack)
    }
    composable(SettingsRoute.LanguageSetting.route) {
        LanguageSettingScreen(onBack)
    }
    composable(SettingsRoute.FontSizeSetting.route) {
        FontSizeSettingScreen(onBack)
    }
}

private fun NavGraphBuilder.notificationNavGraph(
    navController: NavHostController,
    onBack: () -> Unit
) {
    composable(SettingsRoute.NotificationSettings.route) {
        NotificationSettingsScreen(navController, onBack)
    }
    composable(SettingsRoute.NotificationDisplaySetting.route) {
        NotificationDisplaySettingScreen(onBack)
    }
    composable(SettingsRoute.InChatNotificationSetting.route) {
        InChatNotificationSettingsScreen(onBack)
    }
    composable(SettingsRoute.NotificationSoundSetting.route) {
        NotificationSoundSettingScreen(onBack)
    }
}