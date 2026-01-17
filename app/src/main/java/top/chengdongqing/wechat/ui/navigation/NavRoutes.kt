package top.chengdongqing.wechat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector? = null // 二级页面可能没有图标
) {
    object MainShell : Screen("main_shell", "主界面")

    // 主Tab页面
    object Chats : Screen("chats", "消息", Icons.Default.ChatBubble)
    object Contacts : Screen("contacts", "通讯录", Icons.Default.People)
    object Discovery : Screen("discovery", "发现", Icons.Default.Explore)
    object Me : Screen("me", "我", Icons.Default.Person)

    // 二级页面
    object ChatDetail : Screen("chat_detail/{peerId}", "聊天详情")
}

val bottomTabItems = listOf(
    Screen.Chats,
    Screen.Contacts,
    Screen.Discovery,
    Screen.Me
)