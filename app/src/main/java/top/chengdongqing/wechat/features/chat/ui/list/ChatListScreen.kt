package top.chengdongqing.wechat.features.chat.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.ContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.dialog.DialogState
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
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

    val dialog = rememberDialogState()
    val contextMenuState = rememberContextMenuState()
    val overscrollEffect = rememberBounceOverscrollEffect()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.surface)
            .overscroll(overscrollEffect),
        overscrollEffect = overscrollEffect
    ) {
        items(
            items = chats,
            key = { it.sessionId }
        ) { chat ->
            ChatListItem(
                chat = chat,
                onNavigateToDetail = onNavigateToDetail,
                onShowMenu = { position ->
                    val menus = getChatMenuLabels(chat)
                    contextMenuState.show(position, menus, chats.indexOf(chat))
                },
                modifier = Modifier.animateItem() // 置顶/删除时平滑动画
            )
            Box(modifier = Modifier.background(WeTheme.colorScheme.surface)) {
                WeDivider(modifier = Modifier.padding(start = 73.dp))
            }
        }
    }

    ChatContextMenuHandler(contextMenuState, chats, viewModel, dialog)
}

// 单独提取，在长按显示菜单时，只会重绘当前行
@Composable
private fun ChatListItem(
    chat: ChatSession,
    onNavigateToDetail: (String) -> Unit,
    onShowMenu: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(WeTheme.colorScheme.surface)
            .weContextMenu(
                onClick = { onNavigateToDetail(chat.sessionId) },
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
    viewModel: ChatListViewModel,
    dialog: DialogState
) {
    WeContextMenu(state) { targetIndex, menuIndex ->
        val chat = chats.getOrNull(targetIndex) ?: return@WeContextMenu

        when (menuIndex) {
            0 -> viewModel.toggleReadStatus(chat.sessionId, chat.unreadCount > 0)
            1 -> viewModel.stickToTop(chat.sessionId, chat.isPinned)
            2 -> dialog.show(
                title = "不显示聊天后，聊天记录将不会被删除",
                content = "进入聊天详情，可以找回聊天。",
                okText = "我知道了",
                onCancel = null
            ) { viewModel.hideChat(chat.sessionId) }

            3 -> dialog.show(
                title = "删除后，将清空记录同时不显示聊天",
                okText = "删除",
                okColor = Danger
            ) { viewModel.deleteChat(chat.sessionId) }
        }
    }
}