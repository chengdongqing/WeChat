package top.chengdongqing.wechat.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import top.chengdongqing.wechat.ui.addfriend.AddFriendScreen
import top.chengdongqing.wechat.ui.addfriend.PinCodeGroupScreen
import top.chengdongqing.wechat.ui.addfriend.RadarScanScreen
import top.chengdongqing.wechat.ui.chat.session.ChatSessionScreen
import top.chengdongqing.wechat.ui.contacts.detail.ContactDetailScreen
import top.chengdongqing.wechat.ui.home.HomeScreen
import top.chengdongqing.wechat.ui.me.profile.ProfileScreen
import top.chengdongqing.wechat.ui.me.profile.avatar.AvatarScreen
import top.chengdongqing.wechat.ui.me.profile.gender.GenderScreen
import top.chengdongqing.wechat.ui.me.profile.id.IDScreen
import top.chengdongqing.wechat.ui.me.profile.name.NameScreen
import top.chengdongqing.wechat.ui.me.profile.qrcode.QRCodeScreen
import top.chengdongqing.wechat.ui.me.profile.signature.SignatureScreen

@Composable
fun WeChatNavigation(navController: NavHostController = rememberNavController()) {
    val goBack: () -> Unit = {
        navController.popBackStack()
    }

    NavHost(
        navController = navController,
//        startDestination = Screen.ChatSession.createRoute("123"),
        startDestination = Screen.Home.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(Screen.AddFriend.route) {
            AddFriendScreen(
                onNavigateToRadar = {
                    navController.navigate(Screen.RadarScan.route)
                },
                onNavigateToGroup = {
                    navController.navigate(Screen.PinCodeGroup.route)
                },
                onBack = goBack
            )
        }
        composable(Screen.RadarScan.route) {
            RadarScanScreen(goBack)
        }
        composable(Screen.PinCodeGroup.route) {
            PinCodeGroupScreen(goBack)
        }

        composable(
            route = Screen.ChatSession.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatSessionScreen(chatId, goBack)
        }
        composable(
            route = Screen.ContactDetail.route,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ContactDetailScreen(contactId, goBack)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        composable(Screen.Avatar.route) {
            AvatarScreen(goBack)
        }
        composable(Screen.QRCode.route) {
            QRCodeScreen(goBack)
        }
        composable(Screen.ID.route) {
            IDScreen(goBack)
        }
        composable(Screen.Name.route) {
            NameScreen(goBack)
        }
        composable(Screen.Signature.route) {
            SignatureScreen(goBack)
        }
        composable(Screen.Gender.route) {
            GenderScreen(goBack)
        }
    }
}