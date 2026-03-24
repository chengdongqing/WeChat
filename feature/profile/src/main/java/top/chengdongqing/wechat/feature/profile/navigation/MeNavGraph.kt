package top.chengdongqing.wechat.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import top.chengdongqing.wechat.core.common.navigation.ContactsRoute
import top.chengdongqing.wechat.core.common.navigation.MeRoute
import top.chengdongqing.wechat.core.common.navigation.Screen
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.profile.ui.profile.ProfileScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditAvatarScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditGenderScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditIDScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditNameScreen
import top.chengdongqing.wechat.feature.profile.ui.profile.edit.EditSignatureScreen
import top.chengdongqing.wechat.feature.profile.ui.qrcode.QRCodeScreen

fun NavGraphBuilder.meNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(MeRoute.PROFILE) {
        ProfileScreen(navController, onBack)
    }
    composable(MeRoute.QR_CODE) {
        WeTheme(isDark = false) {
            QRCodeScreen(
                onBack = onBack,
                onNavigateToContactDetail = { id ->
                    navController.navigate(ContactsRoute.Detail.createRoute(id))
                },
                onNavigateToPlainText = { text ->
                    navController.navigate(Screen.PlainText.createRoute(text))
                },
                onNavigateToWebView = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                }
            )
        }
    }

    composable(MeRoute.Edit.AVATAR) {
        EditAvatarScreen(onBack)
    }
    composable(MeRoute.Edit.ID) {
        EditIDScreen(onBack)
    }
    composable(MeRoute.Edit.NAME) {
        EditNameScreen(onBack)
    }
    composable(MeRoute.Edit.SIGNATURE) {
        EditSignatureScreen(onBack)
    }
    composable(MeRoute.Edit.GENDER) {
        EditGenderScreen(onBack)
    }
}