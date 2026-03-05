package top.chengdongqing.wechat.features.chat.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import top.chengdongqing.wechat.core.navigation.Screen
import top.chengdongqing.wechat.features.chat.ui.info.ChatInfoScreen
import top.chengdongqing.wechat.features.chat.ui.session.ChatSessionScreen
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.file.FilePreviewScreen
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.MusicPreviewScreen
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

    object FilePreview : ChatRoute("chats/{messageId}/preview/file") {
        const val ARG_MESSAGE_ID = "messageId"

        fun createRoute(messageId: String) = "chats/${messageId}/preview/file"
    }

    object MusicPreview : ChatRoute("chats/{messageId}/preview/music") {
        const val ARG_MESSAGE_ID = "messageId"

        fun createRoute(messageId: String) = "chats/${messageId}/preview/music"
    }
}

fun NavGraphBuilder.chatNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(
        route = ChatRoute.ChatSession.route,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "wechat://chat/{${ChatRoute.ChatSession.ARG_CHAT_ID}}"
            }
        ),
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
            },
            onNavigateToRequestAddFriend = {
                navController.navigate(ContactsRoute.RequestAdd.createRoute(chatId))
            },
            onNavigateToWebView = { url ->
                navController.navigate(Screen.WebView.createRoute(url))
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
    composable(
        route = ChatRoute.MusicPreview.route,
        arguments = listOf(
            navArgument(ChatRoute.MusicPreview.ARG_MESSAGE_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val messageId = backStackEntry.arguments
            ?.getString(ChatRoute.MusicPreview.ARG_MESSAGE_ID) ?: ""
        MusicPreviewScreen(
            onBack = onBack
        )
    }
}