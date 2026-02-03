package top.chengdongqing.wechat.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import top.chengdongqing.wechat.features.chat.ui.info.ChatInfoScreen
import top.chengdongqing.wechat.features.chat.ui.session.ChatSessionScreen
import top.chengdongqing.wechat.features.contacts.addfriend.AddFriendScreen
import top.chengdongqing.wechat.features.contacts.addfriend.PinCodeGroupScreen
import top.chengdongqing.wechat.features.contacts.addfriend.RadarScanScreen
import top.chengdongqing.wechat.features.contacts.detail.ContactDetailScreen
import top.chengdongqing.wechat.features.contacts.detail.ContactSettingScreen
import top.chengdongqing.wechat.features.home.HomeScreen
import top.chengdongqing.wechat.features.me.profile.ProfileScreen
import top.chengdongqing.wechat.features.me.profile.avatar.AvatarScreen
import top.chengdongqing.wechat.features.me.profile.gender.GenderScreen
import top.chengdongqing.wechat.features.me.profile.id.IDScreen
import top.chengdongqing.wechat.features.me.profile.name.NameScreen
import top.chengdongqing.wechat.features.me.profile.qrcode.QRCodeScreen
import top.chengdongqing.wechat.features.me.profile.signature.SignatureScreen
import top.chengdongqing.wechat.features.me.setup.ProfileSetupScreen
import top.chengdongqing.wechat.features.welcome.WelcomeScreen

@Composable
fun WeNavigation(navController: NavHostController = rememberNavController()) {
    val goBack: () -> Unit = { navController.popBackStack() }

    WeNavHost(
        navController = navController,
//        startDestination = Screen.ChatSession.createRoute("123"),
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen {
                navController.navigate(Screen.ProfileSetup.route)
            }
        }
        composable(
            route = Screen.ProfileSetup.route,
            exitTransition = {
                if (targetState.destination.route == Screen.Home.route) {
                    fadeOut(animationSpec = tween(700)) +
                            scaleOut(
                                targetScale = 1.08f,
                                animationSpec = tween(700)
                            )
                } else null
            }
        ) {
            ProfileSetupScreen(onBack = goBack) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Welcome.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }

        composable(
            route = Screen.Home.route,
            enterTransition = {
                if (initialState.destination.route == Screen.ProfileSetup.route) {
                    fadeIn(animationSpec = tween(700)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(700)
                            )
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(300)
                    )
                }
            }
        ) {
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
            ChatSessionScreen(chatId = chatId, onBack = goBack, onNavigateToInfo = {
                navController.navigate(Screen.ChatInfo.createRoute(chatId))
            })
        }
        composable(
            route = Screen.ChatInfo.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatInfoScreen(
                chatId = chatId,
                onBack = goBack,
                onNavigateToContact = { id ->
                    navController.navigate(Screen.ContactDetail.createRoute(id))
                }
            )
        }
        composable(
            route = Screen.ContactDetail.route,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ContactDetailScreen(
                contactId = contactId,
                onBack = goBack,
                onNavigateToChat = { id ->
                    navController.navigate(Screen.ChatSession.createRoute(id))
                },
                onNavigateToSetting = { id ->
                    navController.navigate(Screen.ContactSetting.createRoute(id))
                }
            )
        }
        composable(
            route = Screen.ContactSetting.route,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ContactSettingScreen(contactId, goBack)
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