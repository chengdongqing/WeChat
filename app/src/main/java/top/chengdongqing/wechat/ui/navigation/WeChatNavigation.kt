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
import top.chengdongqing.wechat.ui.common.WeChatScaffold
import top.chengdongqing.wechat.ui.contacts.ContactScreen
import top.chengdongqing.wechat.ui.discovery.DiscoveryScreen
import top.chengdongqing.wechat.ui.me.MeScreen

@Composable
fun WeChatNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.WeChatScaffold.route
    ) {
        composable(Screen.WeChatScaffold.route) {
            WeChatScaffold()
        }
        composable(Screen.Chats.route) {
            ChatListScreen()
        }
        composable(Screen.Contacts.route) {
            ContactScreen()
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