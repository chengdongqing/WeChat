package top.chengdongqing.wechat.feature.chat.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatHistoryPayload
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.chat.ui.info.ChatInfoRoute
import top.chengdongqing.wechat.feature.chat.ui.location.LiveLocationScreen
import top.chengdongqing.wechat.feature.chat.ui.location.LiveLocationViewModel
import top.chengdongqing.wechat.feature.chat.ui.preview.chathistory.ChatHistoryScreen
import top.chengdongqing.wechat.feature.chat.ui.preview.file.FilePreviewScreen
import top.chengdongqing.wechat.feature.chat.ui.preview.music.MusicPreviewScreen
import top.chengdongqing.wechat.feature.chat.ui.session.ChatRoute

fun EntryProviderScope<NavKey>.chatNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    // 聊天会话页
    entry<ScreenRoute.Chat> {
        val chatId = it.chatId

        ChatRoute(chatId)
    }

    // 实时位置页
    entry<ScreenRoute.LiveLocation> {
        LiveLocationScreen(
            onBack = onBack,
            viewModel = hiltViewModel { factory: LiveLocationViewModel.Factory ->
                factory.create(it.chatId)
            }
        )
    }

    // 聊天历史预览页
    entry<ScreenRoute.ChatHistory> { key ->
        val payload = runCatching { Json.decodeFromString<ChatHistoryPayload>(key.payload) }
            .getOrDefault(ChatHistoryPayload("聊天记录", emptyList()))

        ChatHistoryScreen(
            content = MessageContent.ChatHistory(payload.title, payload.items),
            onBack = onBack,
            onOpenHistory = { history ->
                backStack.add(
                    ScreenRoute.ChatHistory(
                        Json.encodeToString(ChatHistoryPayload(history.title, history.items))
                    )
                )
            },
            onOpenFile = { file ->
                backStack.add(
                    ScreenRoute.ChatHistoryFile(
                        path = file.localPath.orEmpty(),
                        filename = file.text,
                        mimeType = file.mimeType ?: "*/*",
                        size = file.fileSize ?: 0
                    )
                )
            },
            onOpenMusic = { music ->
                backStack.add(
                    ScreenRoute.MusicPreview(
                        messageId = "",
                        trackName = Json.encodeToString(music)
                    )
                )
            }
        )
    }

    entry<ScreenRoute.ChatHistoryFile> { file ->
        FilePreviewScreen(
            file = MessageContent.File(
                localPath = file.path,
                filename = file.filename,
                mimeType = file.mimeType,
                size = file.size
            ),
            onBack = onBack
        )
    }

    // 聊天信息页
    entry<ScreenRoute.ChatInfo> {
        ChatInfoRoute(it.chatId)
    }

    // 文件预览页
    entry<ScreenRoute.FilePreview> {
        FilePreviewScreen(
            messageId = it.messageId,
            onBack = onBack
        )
    }

    // 音乐预览页
    entry<ScreenRoute.MusicPreview> { key ->
        val music = runCatching { Json.decodeFromString<MusicTrack>(key.trackName) }
            .recoverCatching { MusicTrack.valueOf(key.trackName) }
            .getOrDefault(MusicTrack.Perfect)

        MusicPreviewScreen(music, onBack)
    }
}
