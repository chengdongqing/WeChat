package top.chengdongqing.wechat.feature.home.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R

sealed class HomeTab(
    val route: String,
    @get:StringRes val label: Int,
    @get:DrawableRes val icon: Int,
    @get:DrawableRes val selectedIcon: Int
) {
    object Chats : HomeTab(
        route = "tab_chats",
        label = R.string.home_tab_wechat,
        icon = R.drawable.ic_tab_chats_outlined,
        selectedIcon = R.drawable.ic_tab_chats_filled
    )

    object Contacts : HomeTab(
        route = "tab_contacts",
        label = R.string.home_tab_contacts,
        icon = R.drawable.ic_tab_contacts_outlined,
        selectedIcon = R.drawable.ic_tab_contacts_filled
    )

    object Discovery : HomeTab(
        route = "tab_discovery",
        label = R.string.home_tab_discovery,
        icon = R.drawable.ic_tab_discover_outlined,
        selectedIcon = R.drawable.ic_tab_discover_filled
    )

    object Me : HomeTab(
        route = "tab_me",
        label = R.string.home_tab_me,
        icon = R.drawable.ic_tab_me_outlined,
        selectedIcon = R.drawable.ic_tab_me_filled
    )

    companion object {
        val tabs = listOf(Chats, Contacts, Discovery, Me)
    }
}