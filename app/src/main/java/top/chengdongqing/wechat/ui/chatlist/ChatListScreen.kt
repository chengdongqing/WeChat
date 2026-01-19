package top.chengdongqing.wechat.ui.chatlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.data.model.Chat
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.ui.components.contextmenu.detectWeContextMenu
import top.chengdongqing.wechat.ui.components.contextmenu.rememberContextMenuState

@Composable
fun ChatListScreen() {
    val chatList = remember {
        listOf(
            Chat(1, "文件传输助手", "已收到文件", "14:20", 0),
            Chat(2, "Compose 学习群", "大家今天学了吗？", "12:05", 0, 5),
            Chat(3, "张三", "晚上一起吃饭吗？", "昨天", 0),
            Chat(4, "微信团队", "你的帐号登录提醒", "星期三", 0),
        )
    }

    val menus = remember {
        listOf("标为未读", "置顶该聊天", "不显示该聊天", "删除该聊天")
    }
    val contextMenuState = rememberContextMenuState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(chatList) { index, chat ->
            Box(
                modifier = Modifier
                    .detectWeContextMenu { position ->
                        contextMenuState.show(position, menus, index)
                    }
            ) {
                ChatItem(chat)
            }
            WeDivider(modifier = Modifier.padding(start = 73.dp))
        }
    }

    WeContextMenu(contextMenuState) { listIndex, menuIndex ->

    }
}