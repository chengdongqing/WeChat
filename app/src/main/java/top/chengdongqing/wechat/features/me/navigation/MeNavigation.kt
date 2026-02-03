package top.chengdongqing.wechat.features.me.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import top.chengdongqing.wechat.features.me.ui.profile.ProfileScreen
import top.chengdongqing.wechat.features.me.ui.profile.edit.EditAvatarScreen
import top.chengdongqing.wechat.features.me.ui.profile.edit.EditGenderScreen
import top.chengdongqing.wechat.features.me.ui.profile.edit.EditIDScreen
import top.chengdongqing.wechat.features.me.ui.profile.edit.EditNameScreen
import top.chengdongqing.wechat.features.me.ui.profile.edit.EditSignatureScreen
import top.chengdongqing.wechat.features.me.ui.qrcode.QRCodeScreen

sealed class MeRoute(val route: String) {
    object Profile : MeRoute("profile")
    object QRCode : MeRoute("qrcode")

    object EditAvatar : MeRoute("profile/avatar")
    object EditName : MeRoute("profile/name")
    object EditID : MeRoute("profile/id")
    object EditSignature : MeRoute("profile/signature")
    object EditGender : MeRoute("profile/gender")
}

fun NavGraphBuilder.meNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(MeRoute.Profile.route) {
        ProfileScreen(navController)
    }
    composable(MeRoute.QRCode.route) {
        QRCodeScreen(onBack)
    }

    composable(MeRoute.EditAvatar.route) {
        EditAvatarScreen(onBack)
    }
    composable(MeRoute.EditID.route) {
        EditIDScreen(onBack)
    }
    composable(MeRoute.EditName.route) {
        EditNameScreen(onBack)
    }
    composable(MeRoute.EditSignature.route) {
        EditSignatureScreen(onBack)
    }
    composable(MeRoute.EditGender.route) {
        EditGenderScreen(onBack)
    }
}