package top.chengdongqing.wechat.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import top.chengdongqing.wechat.core.common.navigation.ContactsRoute
import top.chengdongqing.wechat.core.common.navigation.SettingsRoute
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
    moreNavGraph(navController, onBack)
    connectionModeNavGraph(onBack)
    chatNavGraph(onBack)
    aboutNavGraph(onBack)
}

private fun NavGraphBuilder.aboutNavGraph(
    onBack: () -> Unit
) {
    composable(SettingsRoute.About.route) {
        AboutScreen(onBack)
    }
}

private fun NavGraphBuilder.chatNavGraph(
    onBack: () -> Unit
) {
    composable(SettingsRoute.ChatSettings.route) {
        ChatSettingsScreen(onBack)
    }
    composable(SettingsRoute.ChatManagement.route) {
        ChatManagementScreen(onBack)
    }
}

private fun NavGraphBuilder.connectionModeNavGraph(
    onBack: () -> Unit
) {
    composable(SettingsRoute.ConnectionModeSettings.route) {
        ConnectionModeSettingScreen(onBack)
    }
}

private fun NavGraphBuilder.moreNavGraph(
    navController: NavHostController,
    onBack: () -> Unit
) {
    composable(SettingsRoute.MoreSettings.route) {
        MoreSettingsScreen(navController, onBack)
    }
    composable(SettingsRoute.SystemPermissionSettings.route) {
        SystemPermissionSettingsScreen(onBack)
    }
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
        ContactBlacklistScreen(
            onBack = onBack,
            onNavigateToContactDetail = { id ->
                navController.navigate(ContactsRoute.Detail.createRoute(id))
            }
        )
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
    composable(SettingsRoute.FontScaleSetting.route) {
        ChatTheme {
            FontScaleSettingScreen(onBack)
        }
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
    composable(SettingsRoute.RingtoneSetting.route) {
        RingtoneSettingScreen(onBack)
    }
}