package top.chengdongqing.wechat.features.chat.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import top.chengdongqing.wechat.features.chat.ui.info.ChatInfoScreen
import top.chengdongqing.wechat.features.chat.ui.session.ChatSessionScreen
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.FilePreviewScreen
import top.chengdongqing.wechat.features.contacts.navigation.ContactsRoute

sealed class ChatRoute(val route: String) {
    object ChatSession : ChatRoute("chats/{chatId}") {
        const val ARG_CHAT_ID = "chatId"

        fun createRoute(chatId: String) = "chats/${chatId}"
    }

    object ChatInfo : ChatRoute("chats/{chatId}/info") {
        const val ARG_CHAT_ID = "chatId"

        fun createRoute(chatId: String) = "chats/${chatId}/info"
    }

    object FilePreview : ChatRoute("chats/{chatId}/{messageId}/file") {
        const val ARG_MESSAGE_ID = "messageId"

        fun createRoute(messageId: String) = "chats/{chatId}/${messageId}/file"
    }
}

fun NavGraphBuilder.chatNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(
        route = ChatRoute.ChatSession.route,
        arguments = listOf(
            navArgument(ChatRoute.ChatSession.ARG_CHAT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getString(ChatRoute.ChatSession.ARG_CHAT_ID) ?: ""
        ChatSessionScreen(
            chatId = chatId,
            onBack = onBack,
            onNavigateToInfo = {
                navController.navigate(ChatRoute.ChatInfo.createRoute(chatId))
            },
            onNavigateToContact = { id ->
                navController.navigate(ContactsRoute.Detail.createRoute(id))
            },
            onNavigateToFilePreview = { id ->
                navController.navigate(ChatRoute.FilePreview.createRoute(id))
            }
        )
    }
    composable(
        route = ChatRoute.ChatInfo.route,
        arguments = listOf(
            navArgument(ChatRoute.ChatInfo.ARG_CHAT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getString(ChatRoute.ChatInfo.ARG_CHAT_ID) ?: ""
        ChatInfoScreen(
            chatId = chatId,
            onBack = onBack,
            onNavigateToContact = { id ->
                navController.navigate(ContactsRoute.Detail.createRoute(id))
            }
        )
    }

    composable(
        route = ChatRoute.FilePreview.route,
        arguments = listOf(
            navArgument(ChatRoute.FilePreview.ARG_MESSAGE_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val messageId = backStackEntry.arguments
            ?.getString(ChatRoute.FilePreview.ARG_MESSAGE_ID) ?: ""
        FilePreviewScreen(
            messageId = messageId,
            onBack = onBack
        )
    }
}