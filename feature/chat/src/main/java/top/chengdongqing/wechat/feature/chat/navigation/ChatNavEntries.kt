package top.chengdongqing.wechat.feature.chat.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.common.navigation.ChatKey
import top.chengdongqing.wechat.core.common.navigation.CommonKey
import top.chengdongqing.wechat.core.common.navigation.ContactsKey
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.info.ChatInfoScreen
import top.chengdongqing.wechat.feature.chat.ui.info.ChatInfoViewModel
import top.chengdongqing.wechat.feature.chat.ui.preview.file.FilePreviewScreen
import top.chengdongqing.wechat.feature.chat.ui.preview.music.MusicPreviewScreen
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionScreen
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionViewModel

fun EntryProviderScope<NavKey>.chatNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    // 聊天会话页
    entry<ChatKey.ChatSession> {
        val chatId = it.chatId

        ChatTheme {
            ChatSessionScreen(
                chatId = chatId,
                onBack = onBack,
                onNavigateToInfo = { backStack.add(ChatKey.ChatInfo(chatId)) },
                onNavigateToContact = { id ->
                    backStack.removeIf { key -> key is ContactsKey.Detail }
                    backStack.add(ContactsKey.Detail(id))
                },
                onNavigateToFilePreview = { id -> backStack.add(ChatKey.FilePreview(id)) },
                onNavigateToMusicPreview = { id, name ->
                    backStack.add(
                        ChatKey.MusicPreview(
                            messageId = id,
                            trackName = name
                        )
                    )
                },
                onNavigateToRequestAddFriend = { backStack.add(ContactsKey.RequestAdd(chatId)) },
                onNavigateToWebView = { url -> backStack.add(CommonKey.WebView(url)) },
                viewModel = hiltViewModel { factory: ChatSessionViewModel.Factory ->
                    factory.create(chatId)
                }
            )
        }
    }

    // 聊天信息页
    entry<ChatKey.ChatInfo> {
        val id = it.chatId

        ChatInfoScreen(
            onBack = onBack,
            onNavigateToContact = {
                backStack.removeIf { key -> key is ContactsKey.Detail }
                backStack.add(ContactsKey.Detail(id))
            },
            viewModel = hiltViewModel { factory: ChatInfoViewModel.Factory ->
                factory.create(id)
            }
        )
    }

    // 文件预览页
    entry<ChatKey.FilePreview> {
        FilePreviewScreen(
            messageId = it.messageId,
            onBack = onBack
        )
    }

    // 音乐预览页
    entry<ChatKey.MusicPreview> {
        val music = runCatching { MusicTrack.valueOf(it.trackName) }
            .getOrDefault(MusicTrack.Perfect)

        MusicPreviewScreen(music, onBack)
    }
}