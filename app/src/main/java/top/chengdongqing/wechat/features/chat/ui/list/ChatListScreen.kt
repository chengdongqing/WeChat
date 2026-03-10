package top.chengdongqing.wechat.features.chat.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.ContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.informationbar.InformationBarType
import top.chengdongqing.wechat.core.designsystem.components.informationbar.WeInformationBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppLanguage
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.util.rememberWifiConnected
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession
import top.chengdongqing.wechat.features.settings.domain.model.AppLanguage

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onNavigateToDetail: (sessionId: String) -> Unit
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()

    val isWifiConnected = rememberWifiConnected()
    val contextMenuState = rememberContextMenuState(
        itemWidthDp = when (LocalAppLanguage.current) {
            AppLanguage.English -> 160.dp
            else -> 140.dp
        }
    )
    val overscrollEffect = rememberBounceOverscrollEffect()

    Column {
        if (!isWifiConnected) {
            WeInformationBar(
                type = InformationBarType.TipsWeak,
                message = stringResource(R.string.msg_network_unavailable),
                shape = RectangleShape
            )
            WeDivider()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(WeTheme.colorScheme.surface)
                .overscroll(overscrollEffect),
            overscrollEffect = overscrollEffect
        ) {
            items(
                items = chats,
                key = { it.id }
            ) { chat ->
                val menus = getChatMenuLabels(chat)

                ChatListItem(
                    chat = chat,
                    onNavigateToDetail = onNavigateToDetail,
                    onShowMenu = { position ->
                        contextMenuState.show(position, menus, chats.indexOf(chat))
                    },
                    modifier = Modifier.animateItem()
                )
                Box(
                    modifier = Modifier.background(
                        if (chat.isPinned) {
                            WeTheme.colorScheme.background
                        } else {
                            WeTheme.colorScheme.surface
                        }
                    )
                ) {
                    WeDivider(modifier = Modifier.padding(start = 73.dp))
                }
            }
        }
    }

    ChatContextMenuHandler(contextMenuState, chats, viewModel)
}

@Composable
private fun ChatListItem(
    chat: ChatSession,
    onNavigateToDetail: (String) -> Unit,
    onShowMenu: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (chat.isPinned) WeTheme.colorScheme.background else WeTheme.colorScheme.surface
            )
            .weContextMenu(
                onClick = { onNavigateToDetail(chat.id) },
                onLongClick = { position -> onShowMenu(position) }
            )
    ) {
        ChatItem(chat)
    }
}

@Composable
private fun getChatMenuLabels(chat: ChatSession): List<String> {
    return listOf(
        stringResource(if (chat.unreadCount > 0) R.string.chat_action_mark_read else R.string.chat_action_mark_unread),
        stringResource(if (chat.isPinned) R.string.chat_action_remove_top else R.string.chat_action_sticky_top),
        stringResource(R.string.chat_action_hide),
        stringResource(R.string.chat_action_delete)
    )
}

@Composable
private fun ChatContextMenuHandler(
    state: ContextMenuState,
    chats: List<ChatSession>,
    viewModel: ChatListViewModel
) {
    val dialog = rememberDialogState()
    val hideTitle = stringResource(R.string.chat_hide_hint_title)
    val hideContent = stringResource(R.string.chat_hide_hint_content)
    val deleteHint = stringResource(R.string.chat_delete_hint)

    WeContextMenu(state) { targetIndex, menuIndex ->
        val chat = chats.getOrNull(targetIndex) ?: return@WeContextMenu

        when (menuIndex) {
            0 -> viewModel.toggleReadStatus(chat.id, chat.unreadCount > 0)
            1 -> viewModel.stickToTop(chat.id, chat.isPinned)
            2 -> dialog.show(
                title = hideTitle,
                content = hideContent,
                okText = R.string.action_got_it,
                onCancel = null
            ) { viewModel.hideChat(chat.id) }

            3 -> dialog.show(
                title = deleteHint,
                okText = R.string.action_delete,
                okColor = Danger
            ) { viewModel.deleteChat(chat.id) }
        }
    }
}