package top.chengdongqing.wechat.feature.chat.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import top.chengdongqing.wechat.core.common.navigation.ChatRoute
import top.chengdongqing.wechat.core.common.navigation.ContactsRoute
import top.chengdongqing.wechat.core.common.navigation.Screen
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.info.ChatInfoScreen
import top.chengdongqing.wechat.feature.chat.ui.preview.file.FilePreviewScreen
import top.chengdongqing.wechat.feature.chat.ui.preview.music.MusicPreviewScreen
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionScreen

fun NavGraphBuilder.chatNavGraph(navController: NavHostController, onBack: () -> Unit) {
    composable(
        route = ChatRoute.ChatSession.route,
        arguments = listOf(
            navArgument(ChatRoute.ChatSession.ARG_CHAT_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getString(ChatRoute.ChatSession.ARG_CHAT_ID) ?: ""

        ChatTheme {
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
                onNavigateToMusicPreview = { id, trackName ->
                    navController.navigate(ChatRoute.MusicPreview.createRoute(id, trackName))
                },
                onNavigateToRequestAddFriend = {
                    navController.navigate(ContactsRoute.RequestAdd.createRoute(chatId))
                },
                onNavigateToWebView = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                }
            )
        }
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
            navArgument(ChatRoute.MusicPreview.ARG_MESSAGE_ID) { type = NavType.StringType },
            navArgument(ChatRoute.MusicPreview.ARG_TRACK_NAME) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val trackName =
            backStackEntry.arguments?.getString(ChatRoute.MusicPreview.ARG_TRACK_NAME) ?: ""
        val music = runCatching { MusicTrack.valueOf(trackName) }.getOrDefault(MusicTrack.Perfect)

        MusicPreviewScreen(music, onBack)
    }
}