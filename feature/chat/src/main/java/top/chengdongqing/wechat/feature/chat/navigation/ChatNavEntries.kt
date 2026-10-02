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

fun EntryProviderScope<NavKey>.chatNavEntries(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    entry<ScreenRoute.GroupChat> {
        if (it.groupId.isBlank()) {
            CreateGroupScreen(
                onCreated = { groupId ->
                    backStack.removeLastOrNull()
                    backStack.add(ScreenRoute.Chat(groupId))
                },
                onBack = onBack
            )
        } else {
            ChatTheme {
                ChatSessionScreen(
                    chatId = it.groupId,
                    onBack = onBack,
                    onInfo = { backStack.add(ScreenRoute.GroupInfo(it.groupId)) },
                    onContact = {},
                    onFilePreview = {},
                    onMusicPreview = { _, _ -> },
                    onRequestAddFriend = {},
                    onWebView = {},
                    onFavorites = {
                        backStack.add(ScreenRoute.Favorites(it.groupId))
                    },
                    onChatHistory = { history ->
                        backStack.add(
                            ScreenRoute.ChatHistory(
                                Json.encodeToString(
                                    ChatHistoryPayload(
                                        history.title,
                                        history.items
                                    )
                                )
                            )
                        )
                    },
                    onLive = { liveId, isHost, hostId ->
                        backStack.add(ScreenRoute.LiveRoom(it.groupId, liveId, isHost, hostId))
                    },
                    onLiveLocation = {
                        backStack.add(ScreenRoute.LiveLocation(it.groupId))
                    }
                )
            }
        }
    }

    // 聊天会话页
    entry<ScreenRoute.Chat> {
        val chatId = it.chatId

        ChatTheme {
            ChatSessionScreen(
                chatId = chatId,
                onBack = onBack,
                onInfo = {
                    backStack.add(
                        if (chatId.startsWith("group_")) ScreenRoute.GroupInfo(chatId)
                        else ScreenRoute.ChatInfo(chatId)
                    )
                },
                onContact = { id ->
                    backStack.removeIf { key -> key is ScreenRoute.ContactDetail }
                    backStack.add(ScreenRoute.ContactDetail(id))
                },
                onFilePreview = { id -> backStack.add(ScreenRoute.FilePreview(id)) },
                onMusicPreview = { id, name ->
                    backStack.add(
                        ScreenRoute.MusicPreview(
                            messageId = id,
                            trackName = name
                        )
                    )
                },
                onRequestAddFriend = { backStack.add(ScreenRoute.RequestAddFriend(chatId)) },
                onWebView = { url -> backStack.add(ScreenRoute.WebView(url)) },
                onFavorites = {
                    backStack.add(ScreenRoute.Favorites(chatId))
                },
                onChatHistory = { history ->
                    backStack.add(
                        ScreenRoute.ChatHistory(
                            Json.encodeToString(ChatHistoryPayload(history.title, history.items))
                        )
                    )
                },
                onLive = { liveId, isHost, hostId ->
                    backStack.add(ScreenRoute.LiveRoom(chatId, liveId, isHost, hostId))
                },
                onLiveLocation = {
                    backStack.add(ScreenRoute.LiveLocation(chatId))
                }
            )
        }
    }

    entry<ScreenRoute.LiveRoom> {
        LiveRoomScreen(
            liveId = it.liveId,
            isHost = it.isHost,
            onBack = onBack,
            viewModel = hiltViewModel { factory: LiveRoomViewModel.Factory ->
                factory.create(it.groupId, it.liveId, it.hostId)
            }
        )
    }

    entry<ScreenRoute.LiveLocation> {
        LiveLocationScreen(
            onBack = onBack,
            viewModel = hiltViewModel { factory: LiveLocationViewModel.Factory ->
                factory.create(it.chatId)
            }
        )
    }

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
        val id = it.chatId

        ChatInfoScreen(
            onBack = onBack,
            onContact = {
                backStack.removeIf { key -> key is ScreenRoute.ContactDetail }
                backStack.add(ScreenRoute.ContactDetail(id))
            },
            onRequestAddFriend = {
                backStack.add(ScreenRoute.RequestAddFriend(id))
            },
            onEndTemporaryChat = {
                backStack.removeIf { key ->
                    key is ScreenRoute.ChatInfo ||
                            (key is ScreenRoute.Chat && key.chatId == id)
                }
            },
            viewModel = hiltViewModel { factory: ChatInfoViewModel.Factory ->
                factory.create(id)
            }
        )
    }

    entry<ScreenRoute.GroupInfo> {
        val groupId = it.groupId
        GroupInfoScreen(
            onBack = onBack,
            onExitGroup = {
                backStack.removeIf { key ->
                    key is ScreenRoute.GroupInfo ||
                            (key is ScreenRoute.GroupChat && key.groupId == groupId) ||
                            (key is ScreenRoute.Chat && key.chatId == groupId)
                }
            },
            viewModel = hiltViewModel { factory: GroupInfoViewModel.Factory ->
                factory.create(groupId)
            }
        )
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
