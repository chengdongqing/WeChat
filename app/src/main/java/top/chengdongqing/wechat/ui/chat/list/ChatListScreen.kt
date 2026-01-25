package top.chengdongqing.wechat.ui.chat.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.ui.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.ui.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.ui.components.dialog.rememberDialogState
import top.chengdongqing.wechat.ui.theme.Danger
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.utils.BounceOverscrollEffect

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = viewModel(),
    onNavigateToDetail: (friendId: String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val dialog = rememberDialogState()
    val contextMenuState = rememberContextMenuState()
    val scope = rememberCoroutineScope()
    val overscrollEffect = remember { BounceOverscrollEffect(scope) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WeChatTheme.colorScheme.surface)
            .overscroll(overscrollEffect),
        overscrollEffect = overscrollEffect
    ) {
        itemsIndexed(
            items = state.chats,
            key = { _, chat -> chat.id }
        ) { index, chat ->
            Box(
                modifier = Modifier
                    .weContextMenu({
                        onNavigateToDetail(chat.id)
                    }) { position ->
                        val readMenu = if (chat.unreadCount > 0) "标为已读" else "标为未读"
                        val dynamicMenus =
                            listOf(readMenu, "置顶该聊天", "不显示该聊天", "删除该聊天")
                        contextMenuState.show(position, dynamicMenus, index)
                    }
            ) {
                ChatItem(chat)
            }
            WeDivider(modifier = Modifier.padding(start = 73.dp))
        }
    }

    WeContextMenu(contextMenuState) { targetIndex, menuIndex ->
        when (menuIndex) {
            0 -> viewModel.toggleReadStatus(targetIndex)
            1 -> viewModel.stickToTop(targetIndex)
            2 -> {
                dialog.show(
                    title = "不显示聊天后，聊天记录将不会被删除",
                    content = "通过搜索聊天内容，可以找回聊天。",
                    okText = "我知道了",
                    onCancel = null
                ) {
                    viewModel.hideChat(targetIndex)
                }
            }

            3 -> {
                dialog.show(
                    title = "删除后，将清空记录同时不显示聊天",
                    okText = "删除",
                    okColor = Danger
                ) {
                    viewModel.deleteChat(targetIndex)
                }
            }
        }
    }
}