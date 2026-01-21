package top.chengdongqing.wechat.ui.navigation

import androidx.annotation.DrawableRes
import top.chengdongqing.wechat.R

sealed class Screen(
    val route: String,
    val label: String,
    @get:DrawableRes val iconResId: Int? = null,
    @get:DrawableRes val selectedIconResId: Int? = null
) {
    object Home : Screen("home", "首页")

    // 主Tab页面
    object Chats :
        Screen(
            "chats",
            "微信",
            R.drawable.ic_tab_chats_outline,
            R.drawable.ic_tab_chats_filled
        )

    object Contacts : Screen(
        "contacts",
        "通讯录",
        R.drawable.ic_tab_contacts_outline,
        R.drawable.ic_tab_contacts_filled
    )

    object Discovery : Screen(
        "discovery",
        "发现",
        R.drawable.ic_tab_discover_outline,
        R.drawable.ic_tab_discover_filled
    )

    object Me : Screen(
        "me",
        "我",
        R.drawable.ic_tab_me_outline,
        R.drawable.ic_tab_me_filled
    )

    // 二级页面
    object AddFriend : Screen("add_friend", "添加朋友")
    object RadarScan : Screen("radar_scan", "雷达扫描")
    object PinCodeGroup : Screen("pin_code_group", "面对面建群")
    object ChatDetail : Screen("chat_detail/{peerId}", "聊天详情")
}