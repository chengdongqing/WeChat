package top.chengdongqing.wechat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
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