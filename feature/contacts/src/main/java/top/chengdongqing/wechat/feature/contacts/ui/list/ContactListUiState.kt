package top.chengdongqing.wechat.feature.contacts.ui.list

import top.chengdongqing.wechat.core.model.ContactItem

data class ContactListUiState(
    val isLoading: Boolean = true,
    val groups: Map<Char, List<ContactItem>> = emptyMap(),
    val totalCount: Int = 0,
    val indexMap: Map<Char, Int> = emptyMap(),
    val unreadCount: Int = 0
)