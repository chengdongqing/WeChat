package top.chengdongqing.wechat.features.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.Screen
import top.chengdongqing.wechat.features.contacts.navigation.ContactsRoute
import top.chengdongqing.wechat.features.profile.ui.profile.ProfileScreen
import top.chengdongqing.wechat.features.profile.ui.profile.edit.EditAvatarScreen
import top.chengdongqing.wechat.features.profile.ui.profile.edit.EditGenderScreen
import top.chengdongqing.wechat.features.profile.ui.profile.edit.EditIDScreen
import top.chengdongqing.wechat.features.profile.ui.profile.edit.EditNameScreen
import top.chengdongqing.wechat.features.profile.ui.profile.edit.EditSignatureScreen
import top.chengdongqing.wechat.features.profile.ui.qrcode.QRCodeScreen

object MeRoute {
    private const val ROOT = "me"
    const val PROFILE = "$ROOT/profile"
    const val QR_CODE = "$ROOT/qrcode"

    // 编辑个人信息的子路由
    object Edit {
        private const val EDIT_ROOT = "$PROFILE/edit"
        const val AVATAR = "$EDIT_ROOT/avatar"
        const val NAME = "$EDIT_ROOT/name"
        const val ID = "$EDIT_ROOT/id"
        const val SIGNATURE = "$EDIT_ROOT/signature"
        const val GENDER = "$EDIT_ROOT/gender"
    }
}

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