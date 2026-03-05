package top.chengdongqing.wechat.features.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import top.chengdongqing.wechat.features.settings.ui.SettingsScreen
import top.chengdongqing.wechat.features.settings.ui.notification.InChatNotificationSettingsScreen
import top.chengdongqing.wechat.features.settings.ui.notification.NotificationDisplaySettingScreen
import top.chengdongqing.wechat.features.settings.ui.notification.NotificationSettingsScreen
import top.chengdongqing.wechat.features.settings.ui.notification.NotificationSoundSettingScreen

sealed class SettingsRoute(val route: String) {
    object Settings : SettingsRoute("settings")
    object NotificationSettings : SettingsRoute("settings/notification")
    object NotificationDisplaySettings : SettingsRoute("settings/notification/display")
    object InChatNotificationSettings : SettingsRoute("settings/notification/in_chat")
    object NotificationSoundSetting : SettingsRoute("settings/notification/sound")
}

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    onBack: () -> Unit
) {
    composable(SettingsRoute.Settings.route) {
        SettingsScreen(navController, onBack)
    }
    composable(SettingsRoute.NotificationSettings.route) {
        NotificationSettingsScreen(navController, onBack)
    }
    composable(SettingsRoute.NotificationDisplaySettings.route) {
        NotificationDisplaySettingScreen(onBack)
    }
    composable(SettingsRoute.InChatNotificationSettings.route) {
        InChatNotificationSettingsScreen(onBack)
    }
    composable(SettingsRoute.NotificationSoundSetting.route) {
        NotificationSoundSettingScreen(onBack)
    }
}