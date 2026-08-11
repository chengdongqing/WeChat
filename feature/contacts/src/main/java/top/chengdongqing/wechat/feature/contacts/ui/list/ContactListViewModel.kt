package top.chengdongqing.wechat.feature.contacts.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.core.data.mapper.toListItem
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.ContactItem
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.model.toContact
import javax.inject.Inject

@HiltViewModel
class ContactListViewModel @Inject constructor(
    contactRepository: ContactRepository,
    profileRepository: ProfileRepository,
    friendRequestRepository: FriendRequestRepository
) : ViewModel() {
    /**
     * 组合多个数据流
     */
    val state: StateFlow<ContactListUiState> = combine(
        contactRepository.observeAllContacts(),
        profileRepository.observeProfile(),
        friendRequestRepository.observeUnreadCount()
    ) { contacts, myProfile, unreadCount ->
        // 将自己插入到联系人列表
        val allContacts = if (myProfile != null) {
            contacts + myProfile.toContact() + LocalAiAssistant.toContact()
        } else {
            contacts
        }

        // 根据首字母分组
        val groups = allContacts.groupByInitial()
        // 计算索引映射
        val indexMap = calculateIndexMap(groups)

        ContactListUiState(
            isLoading = false,
            groups = groups,
            totalCount = allContacts.size + 1,
            indexMap = indexMap,
            unreadCount = unreadCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContactListUiState(isLoading = true)
    )

    /**
     * 按字母分组
     */
    private suspend fun List<Contact>.groupByInitial(): Map<Char, List<ContactItem>> =
        this
            .toListItem()
            .groupBy { it.initial }
            .toSortedMap { a, b ->
                when {
                    a == '#' -> 1 // # 放最后
                    b == '#' -> -1
                    else -> a.compareTo(b)
                }
            }

    /**
     * 计算每个首字母对应的列表索引
     */
    private fun calculateIndexMap(groups: Map<Char, List<ContactItem>>): Map<Char, Int> {
        val indexMap = mutableMapOf<Char, Int>()
        var currentIndex = 1 // 顶部功能项占 1 个位置

        groups.forEach { (initial, contacts) ->
            indexMap[initial] = currentIndex
            // 每一个分组消耗：1 (Header) + N (Contacts)
            currentIndex += (contacts.size + 1)
        }
        return indexMap
    }
}

// UI State
data class ContactListUiState(
    val isLoading: Boolean = true,
    val groups: Map<Char, List<ContactItem>> = emptyMap(),
    val totalCount: Int = 0,
    val indexMap: Map<Char, Int> = emptyMap(),
    val unreadCount: Int = 0
)
