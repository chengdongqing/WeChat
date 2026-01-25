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
import top.chengdongqing.wechat.ui.home.HomeScreen

@Composable
fun WeChatNavigation(navController: NavHostController = rememberNavController()) {
    fun goBack() {
        navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.ChatDetail.createRoute("123"),
//        startDestination = Screen.Home.route,
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
                }
            ) { goBack() }
        }
        composable(Screen.RadarScan.route) {
            RadarScanScreen { goBack() }
        }
        composable(Screen.PinCodeGroup.route) {
            PinCodeGroupScreen { goBack() }
        }
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("friendId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            ChatSessionScreen(friendId) { goBack() }
        }
    }
}