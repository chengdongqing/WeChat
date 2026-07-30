package top.chengdongqing.wechat.feature.chat.ui.list

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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.ContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.informationbar.InformationBarType
import top.chengdongqing.wechat.core.designsystem.components.informationbar.WeInformationBar
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.runtime.rememberBluetoothEnabled
import top.chengdongqing.wechat.core.designsystem.runtime.rememberWifiConnected
import top.chengdongqing.wechat.core.designsystem.runtime.rememberWifiEnabled
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.SemanticError
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.AppLanguage
import top.chengdongqing.wechat.core.model.ChatSession

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onNavigateToDetail: (sessionId: String) -> Unit
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()

    val contextMenuState = rememberContextMenuState(
        itemWidthDp = when (LocalAppearanceSetting.current.appLanguage) {
            AppLanguage.English -> 160.dp
            else -> 140.dp
        }
    )
    val overscrollEffect = rememberBounceOverscrollEffect()

    /**
     * 注册当前是否在聊天列表的状态
     */
    LifecycleResumeEffect(Unit) {
        viewModel.activeSessionManager.enterList()

        onPauseOrDispose {
            viewModel.activeSessionManager.leaveList()
        }
    }

    Column {
        ConnectionErrorBar(viewModel)

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
private fun ConnectionErrorBar(viewModel: ChatListViewModel) {
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()

    val errorMessage = when (connectionMode) {
        ConnectionMode.WiFiLan -> stringResource(R.string.msg_wifi_disconnected).takeUnless { rememberWifiConnected() }
        ConnectionMode.WiFiDirect -> stringResource(R.string.msg_wifi_disabled).takeUnless { rememberWifiEnabled() }
        ConnectionMode.Bluetooth -> stringResource(R.string.msg_bluetooth_disabled).takeUnless { rememberBluetoothEnabled() }
    }

    if (errorMessage != null) {
        WeInformationBar(
            type = InformationBarType.TipsWeak,
            message = errorMessage,
            shape = RectangleShape
        )
        WeDivider()
    }
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
                okColor = SemanticError
            ) { viewModel.deleteChat(chat.id) }
        }
    }
}