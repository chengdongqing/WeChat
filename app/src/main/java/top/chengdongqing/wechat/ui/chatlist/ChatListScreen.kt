package top.chengdongqing.wechat.ui.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.utils.showToast
import top.chengdongqing.wechat.data.model.Chat
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.ui.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.ui.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.ui.components.dialog.rememberDialogState
import top.chengdongqing.wechat.ui.theme.Danger
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun ChatListScreen() {
    val chatList = remember {
        generateMockChats()
    }

    val menus = remember {
        listOf("标为未读", "置顶该聊天", "不显示该聊天", "删除该聊天")
    }
    val contextMenuState = rememberContextMenuState()
    val dialog = rememberDialogState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WeChatTheme.colorScheme.surface)
    ) {
        itemsIndexed(
            items = chatList,
            key = { _, chat -> chat.id }
        ) { index, chat ->
            Box(
                modifier = Modifier
                    .weContextMenu(
                        onClick = {
                            context.showToast("你好")
                        }
                    ) { position ->
                        contextMenuState.show(position, menus, index)
                    }
            ) {
                ChatItem(chat)
            }
            WeDivider(modifier = Modifier.padding(start = 73.dp))
        }
    }

    WeContextMenu(contextMenuState) { _, menuIndex ->
        when (menuIndex) {
            2 -> {
                dialog.show(
                    title = "不显示聊天后，聊天记录将不会被删除",
                    content = "通过搜索聊天内容，可以找回聊天。",
                    okText = "我知道了",
                    onCancel = null
                )
            }

            3 -> {
                dialog.show(
                    title = "删除后，将清空记录同时不显示聊天",
                    okText = "删除",
                    okColor = Danger
                )
            }
        }
    }
}

private fun generateMockChats(count: Int = 100): List<Chat> {
    val names = listOf("张三", "李四", "王五", "Compose 交流群", "文件传输助手", "GitHub 通知")
    val messages = listOf("好的", "吃了没？", "[图片]", "有人在吗？", "代码已提交", "明天见")

    return List(count) { i ->
        Chat(
            id = i,
            name = "${names[i % names.size]} $i",
            lastMessage = messages[i % messages.size],
            time = "${12}:${(10 + i % 50).toString().padStart(2, '0')}",
            avatarRes = 0,
            unreadCount = if (i % 9 == 0) i * 2 else 0
        )
    }
}