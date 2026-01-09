package top.chengdongqing.wechat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import top.chengdongqing.wechat.ui.chat.ChatScreen
import top.chengdongqing.wechat.ui.chat.ChatViewModel
import top.chengdongqing.wechat.ui.discovery.DiscoveryScreen

@Composable
fun AppNavigation(viewModel: ChatViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "discovery"
    ) {
        // 发现设备页
        composable("discovery") {
            DiscoveryScreen(
                viewModel = viewModel,
                onNavigateToChat = { peerId ->
                    navController.navigate("chat/$peerId")
                }
            )
        }

        // 聊天对话页
        composable(
            route = "chat/{peerId}",
            arguments = listOf(
                navArgument("peerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""

            ChatScreen(
                viewModel = viewModel,
                peerId = peerId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}