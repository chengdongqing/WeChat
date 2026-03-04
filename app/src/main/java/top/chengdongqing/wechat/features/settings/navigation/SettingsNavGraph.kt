package top.chengdongqing.wechat.features.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import top.chengdongqing.wechat.features.settings.ui.SettingsScreen

sealed class SettingsRoute(val route: String) {
    object Settings : SettingsRoute("settings")
}

fun NavGraphBuilder.settingsNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(SettingsRoute.Settings.route) {
        SettingsScreen(
            onBack = onBack
        )
    }
}