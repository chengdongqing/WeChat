package top.chengdongqing.wechat.app.shell

import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar.NavigationTab

enum class MainTab(
    override val labelRes: Int,
    override val iconRes: Int,
    override val selectedIconRes: Int
) : NavigationTab {
    Chats(
        labelRes = R.string.home_tab_wechat,
        iconRes = R.drawable.ic_tab_chats_outlined,
        selectedIconRes = R.drawable.ic_tab_chats_filled
    ),
    Contacts(
        labelRes = R.string.home_tab_contacts,
        iconRes = R.drawable.ic_tab_contacts_outlined,
        selectedIconRes = R.drawable.ic_tab_contacts_filled
    ),
    Discovery(
        labelRes = R.string.home_tab_discovery,
        iconRes = R.drawable.ic_tab_discover_outlined,
        selectedIconRes = R.drawable.ic_tab_discover_filled
    ),
    Me(
        labelRes = R.string.home_tab_me,
        iconRes = R.drawable.ic_tab_me_outlined,
        selectedIconRes = R.drawable.ic_tab_me_filled
    )
}