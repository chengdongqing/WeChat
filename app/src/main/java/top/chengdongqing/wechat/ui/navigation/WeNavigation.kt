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
import top.chengdongqing.wechat.ui.chatdetail.ChatDetailScreen
import top.chengdongqing.wechat.ui.chatlist.ChatListScreen
import top.chengdongqing.wechat.ui.contacts.ContactsScreen
import top.chengdongqing.wechat.ui.discovery.DiscoveryScreen
import top.chengdongqing.wechat.ui.home.HomeScreen
import top.chengdongqing.wechat.ui.me.MeScreen

@Composable
fun WeChatNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.AddFriend.route,
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
        composable(Screen.Chats.route) {
            ChatListScreen()
        }
        composable(Screen.Contacts.route) {
            ContactsScreen()
        }
        composable(Screen.Discovery.route) {
            DiscoveryScreen()
        }
        composable(Screen.Me.route) {
            MeScreen()
        }

        composable(Screen.AddFriend.route) {
            AddFriendScreen(
                onNavigateToRadar = {
                    navController.navigate(Screen.RadarScan.route)
                },
                onNavigateToGroup = {
                    navController.navigate(Screen.PinCodeGroup.route)
                }
            ) {
                navController.popBackStack()
            }
        }
        composable(Screen.RadarScan.route) {
            RadarScanScreen {
                navController.popBackStack()
            }
        }
        composable(Screen.PinCodeGroup.route) {
            PinCodeGroupScreen {
                navController.popBackStack()
            }
        }
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("peerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            ChatDetailScreen(peerId)
        }
    }
}