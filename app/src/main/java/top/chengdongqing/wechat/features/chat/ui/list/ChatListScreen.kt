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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.ContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.informationbar.InformationBarType
import top.chengdongqing.wechat.core.designsystem.components.informationbar.WeInformationBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onNavigateToDetail: (sessionId: String) -> Unit
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()

    val contextMenuState = rememberContextMenuState()
    val overscrollEffect = rememberBounceOverscrollEffect()

    Column {
        WeInformationBar(
            type = InformationBarType.TipsWeak,
            message = "当前无法连接网络，可检查网络设置是否正常。",
            shape = RectangleShape
        )
        WeDivider()

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
                ChatListItem(
                    chat = chat,
                    onNavigateToDetail = onNavigateToDetail,
                    onShowMenu = { position ->
                        val menus = getChatMenuLabels(chat)
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

private fun getChatMenuLabels(chat: ChatSession): List<String> {
    return listOf(
        if (chat.unreadCount > 0) "标为已读" else "标为未读",
        if (chat.isPinned) "取消置顶" else "置顶该聊天",
        "不显示该聊天",
        "删除该聊天"
    )
}

@Composable
private fun ChatContextMenuHandler(
    state: ContextMenuState,
    chats: List<ChatSession>,
    viewModel: ChatListViewModel
) {
    val dialog = rememberDialogState()

    WeContextMenu(state) { targetIndex, menuIndex ->
        val chat = chats.getOrNull(targetIndex) ?: return@WeContextMenu

        when (menuIndex) {
            0 -> viewModel.toggleReadStatus(chat.id, chat.unreadCount > 0)
            1 -> viewModel.stickToTop(chat.id, chat.isPinned)
            2 -> dialog.show(
                title = "不显示聊天后，聊天记录将不会被删除",
                content = "进入聊天详情，可以找回聊天。",
                okText = "我知道了",
                onCancel = null
            ) { viewModel.hideChat(chat.id) }

            3 -> dialog.show(
                title = "删除后，将清空记录同时不显示聊天",
                okText = "删除",
                okColor = Danger
            ) { viewModel.deleteChat(chat.id) }
        }
    }
}