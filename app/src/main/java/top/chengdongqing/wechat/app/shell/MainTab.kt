package top.chengdongqing.wechat.app.shell

import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar.NavigationTab
import top.chengdongqing.wechat.R as AppR

enum class MainTab(
    override val labelRes: Int,
    override val iconRes: Int,
    override val selectedIconRes: Int
) : NavigationTab {
    Chats(
        labelRes = AppR.string.home_tab_wechat,
        iconRes = R.drawable.ic_tab_chats_outlined,
        selectedIconRes = R.drawable.ic_tab_chats_filled
    ),
    Contacts(
        labelRes = AppR.string.home_tab_contacts,
        iconRes = R.drawable.ic_tab_contacts_outlined,
        selectedIconRes = R.drawable.ic_tab_contacts_filled
    ),
    Discovery(
        labelRes = AppR.string.home_tab_discovery,
        iconRes = R.drawable.ic_tab_discover_outlined,
        selectedIconRes = R.drawable.ic_tab_discover_filled
    ),
    Me(
        labelRes = AppR.string.home_tab_me,
        iconRes = R.drawable.ic_tab_me_outlined,
        selectedIconRes = R.drawable.ic_tab_me_filled
    )
}
