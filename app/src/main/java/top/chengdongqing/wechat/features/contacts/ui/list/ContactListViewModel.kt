package top.chengdongqing.wechat.features.contacts.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.core.util.PinyinHelper.getInitial
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.ContactRelation
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
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
        profileRepository.getCurrentProfile(),
        friendRequestRepository.observeUnreadCount()
    ) { contacts, myProfile, unreadCount ->
        // 动态合并：将自己插入到联系人列表
        val allContacts = if (myProfile != null) {
            val myselfAsContact = Contact(
                id = myProfile.id,
                nickname = myProfile.nickname,
                avatarPath = myProfile.avatarPath,
                signature = myProfile.signature,
                gender = myProfile.gender,
                relation = ContactRelation.Myself
            )
            // 合并列表（自己 + 其他联系人）
            listOf(myselfAsContact) + contacts
        } else {
            contacts
        }

        // 处理联系人数据
        val groups = allContacts.groupByInitial()
        val indexMap = calculateIndexMap(groups)

        ContactListUiState(
            isLoading = false,
            groups = groups,
            totalCount = allContacts.size,
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
    private fun List<Contact>.groupByInitial(): Map<Char, List<ContactItem>> =
        this
            .map { contact ->
                ContactItem(
                    id = contact.id,
                    name = contact.displayName,
                    note = contact.note,
                    avatarPath = contact.avatarPath,
                    isMyself = contact.isMyself,
                    initial = contact.displayName.getInitial()
                )
            }
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

// 联系人列表项
data class ContactItem(
    val id: String,
    val name: String,
    val note: String?,
    val avatarPath: String?,
    val isMyself: Boolean,
    val initial: Char,
)

// UI State
data class ContactListUiState(
    val isLoading: Boolean = true,
    val groups: Map<Char, List<ContactItem>> = emptyMap(),
    val totalCount: Int = 0,
    val indexMap: Map<Char, Int> = emptyMap(),
    val unreadCount: Int = 0
)