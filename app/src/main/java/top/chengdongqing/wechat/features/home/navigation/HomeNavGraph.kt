package top.chengdongqing.wechat.features.home.navigation

import androidx.annotation.DrawableRes
import top.chengdongqing.wechat.R

sealed class HomeTab(
    val route: String,
    val label: String,
    @get:DrawableRes val icon: Int,
    @get:DrawableRes val selectedIcon: Int
) {
    object Chats : HomeTab(
        route = "tab_chats",
        label = "微信",
        icon = R.drawable.ic_tab_chats_outlined,
        selectedIcon = R.drawable.ic_tab_chats_filled
    )

    object Contacts : HomeTab(
        route = "tab_contacts",
        label = "通讯录",
        icon = R.drawable.ic_tab_contacts_outlined,
        selectedIcon = R.drawable.ic_tab_contacts_filled
    )

    object Discovery : HomeTab(
        route = "tab_discovery",
        label = "发现",
        icon = R.drawable.ic_tab_discover_outlined,
        selectedIcon = R.drawable.ic_tab_discover_filled
    )

    object Me : HomeTab(
        route = "tab_me",
        label = "我",
        icon = R.drawable.ic_tab_me_outlined,
        selectedIcon = R.drawable.ic_tab_me_filled
    )

    companion object {
        val tabs = listOf(Chats, Contacts, Discovery, Me)
    }
}