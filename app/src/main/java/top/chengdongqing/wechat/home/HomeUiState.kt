package top.chengdongqing.wechat.home

import androidx.compose.runtime.Immutable
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar.NavigationTab
import top.chengdongqing.wechat.R as AppR

enum class HomeTab(
    override val label: Int,
    override val icon: Int,
    override val selectedIcon: Int
) : NavigationTab {
    Chats(
        label = AppR.string.home_tab_chats,
        icon = R.drawable.ic_tab_chats_outlined,
        selectedIcon = R.drawable.ic_tab_chats_filled
    ),
    Contacts(
        label = AppR.string.home_tab_contacts,
        icon = R.drawable.ic_tab_contacts_outlined,
        selectedIcon = R.drawable.ic_tab_contacts_filled
    ),
    Discover(
        label = AppR.string.home_tab_discover,
        icon = R.drawable.ic_tab_discover_outlined,
        selectedIcon = R.drawable.ic_tab_discover_filled
    ),
    Me(
        label = AppR.string.home_tab_me,
        icon = R.drawable.ic_tab_me_outlined,
        selectedIcon = R.drawable.ic_tab_me_filled
    )
}

@Immutable
data class HomeUiState(
    val currentTab: HomeTab = HomeTab.Chats,
    val unreadMap: Map<HomeTab, Int> = emptyMap()
)