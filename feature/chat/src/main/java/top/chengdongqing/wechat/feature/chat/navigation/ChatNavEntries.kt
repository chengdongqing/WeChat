package top.chengdongqing.wechat.feature.chat.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.group.CreateGroupScreen
import top.chengdongqing.wechat.feature.chat.ui.group.GroupInfoScreen
import top.chengdongqing.wechat.feature.chat.ui.group.GroupInfoViewModel
import top.chengdongqing.wechat.feature.chat.ui.info.ChatInfoScreen
import top.chengdongqing.wechat.feature.chat.ui.info.ChatInfoViewModel
import top.chengdongqing.wechat.feature.chat.ui.live.LiveRoomScreen
import top.chengdongqing.wechat.feature.chat.ui.live.LiveRoomViewModel
import top.chengdongqing.wechat.feature.chat.ui.preview.file.FilePreviewScreen
import top.chengdongqing.wechat.feature.chat.ui.preview.music.MusicPreviewScreen
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionScreen
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionViewModel

fun EntryProviderScope<NavKey>.chatNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<NavigationKey.GroupChat> {
        if (it.groupId.isBlank()) {
            CreateGroupScreen(
                onCreated = { groupId ->
                    backStack.removeLastOrNull()
                    backStack.add(NavigationKey.ChatSession(groupId))
                },
                onBack = onBack
            )
        } else {
            ChatTheme {
                ChatSessionScreen(
                    chatId = it.groupId,
                    onBack = onBack,
                    onNavigateToInfo = { backStack.add(NavigationKey.GroupInfo(it.groupId)) },
                    onNavigateToContact = {},
                    onNavigateToFilePreview = {},
                    onNavigateToMusicPreview = { _, _ -> },
                    onNavigateToRequestAddFriend = {},
                    onNavigateToWebView = {},
                    onNavigateToLive = { liveId, isHost, hostId ->
                        backStack.add(NavigationKey.LiveRoom(it.groupId, liveId, isHost, hostId))
                    },
                    viewModel = hiltViewModel { factory: ChatSessionViewModel.Factory ->
                        factory.create(it.groupId)
                    }
                )
            }
        }
    }

    // 聊天会话页
    entry<NavigationKey.ChatSession> {
        val chatId = it.chatId

        ChatTheme {
            ChatSessionScreen(
                chatId = chatId,
                onBack = onBack,
                onNavigateToInfo = {
                    backStack.add(
                        if (chatId.startsWith("group_")) NavigationKey.GroupInfo(chatId)
                        else NavigationKey.ChatInfo(chatId)
                    )
                },
                onNavigateToContact = { id ->
                    backStack.removeIf { key -> key is NavigationKey.ContactDetail }
                    backStack.add(NavigationKey.ContactDetail(id))
                },
                onNavigateToFilePreview = { id -> backStack.add(NavigationKey.FilePreview(id)) },
                onNavigateToMusicPreview = { id, name ->
                    backStack.add(
                        NavigationKey.MusicPreview(
                            messageId = id,
                            trackName = name
                        )
                    )
                },
                onNavigateToRequestAddFriend = { backStack.add(NavigationKey.RequestAddFriend(chatId)) },
                onNavigateToWebView = { url -> backStack.add(NavigationKey.WebView(url)) },
                onNavigateToLive = { liveId, isHost, hostId ->
                    backStack.add(NavigationKey.LiveRoom(chatId, liveId, isHost, hostId))
                },
                viewModel = hiltViewModel { factory: ChatSessionViewModel.Factory ->
                    factory.create(chatId)
                }
            )
        }
    }

    entry<NavigationKey.LiveRoom> {
        LiveRoomScreen(
            liveId = it.liveId,
            isHost = it.isHost,
            onBack = onBack,
            viewModel = hiltViewModel { factory: LiveRoomViewModel.Factory ->
                factory.create(it.groupId, it.liveId, it.hostId)
            }
        )
    }

    // 聊天信息页
    entry<NavigationKey.ChatInfo> {
        val id = it.chatId

        ChatInfoScreen(
            onBack = onBack,
            onNavigateToContact = {
                backStack.removeIf { key -> key is NavigationKey.ContactDetail }
                backStack.add(NavigationKey.ContactDetail(id))
            },
            viewModel = hiltViewModel { factory: ChatInfoViewModel.Factory ->
                factory.create(id)
            }
        )
    }

    entry<NavigationKey.GroupInfo> {
        val groupId = it.groupId
        GroupInfoScreen(
            onBack = onBack,
            onExitGroup = {
                backStack.removeIf { key ->
                    key is NavigationKey.GroupInfo ||
                        (key is NavigationKey.GroupChat && key.groupId == groupId) ||
                        (key is NavigationKey.ChatSession && key.chatId == groupId)
                }
            },
            viewModel = hiltViewModel { factory: GroupInfoViewModel.Factory ->
                factory.create(groupId)
            }
        )
    }

    // 文件预览页
    entry<NavigationKey.FilePreview> {
        FilePreviewScreen(
            messageId = it.messageId,
            onBack = onBack
        )
    }

    // 音乐预览页
    entry<NavigationKey.MusicPreview> { key ->
        val music = runCatching { Json.decodeFromString<MusicTrack>(key.trackName) }
            .recoverCatching { MusicTrack.valueOf(key.trackName) }
            .getOrDefault(MusicTrack.Perfect)

        MusicPreviewScreen(music, onBack)
    }
}
