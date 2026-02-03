package top.chengdongqing.wechat.features.chat.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onNavigateToDetail: (friendId: String) -> Unit
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()

    val dialog = rememberDialogState()
    val contextMenuState = rememberContextMenuState()
    val overscrollEffect = rememberBounceOverscrollEffect()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.background)
            .overscroll(overscrollEffect),
        overscrollEffect = overscrollEffect
    ) {
        itemsIndexed(
            items = chats,
            key = { _, chat -> chat.id }
        ) { index, chat ->
            Box(
                modifier = Modifier
                    .background(WeTheme.colorScheme.surface)
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
            Box(modifier = Modifier.background(WeTheme.colorScheme.surface)) {
                WeDivider(modifier = Modifier.padding(start = 73.dp))
            }
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