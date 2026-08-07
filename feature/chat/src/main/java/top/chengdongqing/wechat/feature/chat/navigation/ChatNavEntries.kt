package top.chengdongqing.wechat.feature.chat.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatHistoryPayload
import top.chengdongqing.wechat.core.data.model.MessageContent
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
import top.chengdongqing.wechat.feature.chat.ui.location.LiveLocationScreen
import top.chengdongqing.wechat.feature.chat.ui.location.LiveLocationViewModel
import top.chengdongqing.wechat.feature.chat.ui.preview.chathistory.ChatHistoryScreen
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
                    onNavigateToFavorites = {
                        backStack.add(NavigationKey.Favorites(it.groupId))
                    },
                    onNavigateToChatHistory = { history ->
                        backStack.add(
                            NavigationKey.ChatHistory(
                                Json.encodeToString(
                                    ChatHistoryPayload(
                                        history.title,
                                        history.items
                                    )
                                )
                            )
                        )
                    },
                    onNavigateToLive = { liveId, isHost, hostId ->
                        backStack.add(NavigationKey.LiveRoom(it.groupId, liveId, isHost, hostId))
                    },
                    onNavigateToLiveLocation = {
                        backStack.add(NavigationKey.LiveLocation(it.groupId))
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
                onNavigateToFavorites = {
                    backStack.add(NavigationKey.Favorites(chatId))
                },
                onNavigateToChatHistory = { history ->
                    backStack.add(
                        NavigationKey.ChatHistory(
                            Json.encodeToString(ChatHistoryPayload(history.title, history.items))
                        )
                    )
                },
                onNavigateToLive = { liveId, isHost, hostId ->
                    backStack.add(NavigationKey.LiveRoom(chatId, liveId, isHost, hostId))
                },
                onNavigateToLiveLocation = {
                    backStack.add(NavigationKey.LiveLocation(chatId))
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

    entry<NavigationKey.LiveLocation> {
        LiveLocationScreen(
            onBack = onBack,
            viewModel = hiltViewModel { factory: LiveLocationViewModel.Factory ->
                factory.create(it.chatId)
            }
        )
    }

    entry<NavigationKey.ChatHistory> { key ->
        val payload = runCatching { Json.decodeFromString<ChatHistoryPayload>(key.payload) }
            .getOrDefault(ChatHistoryPayload("聊天记录", emptyList()))

        ChatHistoryScreen(
            content = MessageContent.ChatHistory(payload.title, payload.items),
            onBack = onBack,
            onOpenHistory = { history ->
                backStack.add(
                    NavigationKey.ChatHistory(
                        Json.encodeToString(ChatHistoryPayload(history.title, history.items))
                    )
                )
            },
            onOpenFile = { file ->
                backStack.add(
                    NavigationKey.ChatHistoryFile(
                        path = file.localPath.orEmpty(),
                        filename = file.text,
                        mimeType = file.mimeType ?: "*/*",
                        size = file.fileSize ?: 0
                    )
                )
            },
            onOpenMusic = { music ->
                backStack.add(
                    NavigationKey.MusicPreview(
                        messageId = "",
                        trackName = Json.encodeToString(music)
                    )
                )
            }
        )
    }

    entry<NavigationKey.ChatHistoryFile> { file ->
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
    entry<NavigationKey.ChatInfo> {
        val id = it.chatId

        ChatInfoScreen(
            onBack = onBack,
            onNavigateToContact = {
                backStack.removeIf { key -> key is NavigationKey.ContactDetail }
                backStack.add(NavigationKey.ContactDetail(id))
            },
            onRequestAddFriend = {
                backStack.add(NavigationKey.RequestAddFriend(id))
            },
            onEndTemporaryChat = {
                backStack.removeIf { key ->
                    key is NavigationKey.ChatInfo ||
                            (key is NavigationKey.ChatSession && key.chatId == id)
                }
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
