package top.chengdongqing.wechat.features.contacts.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import top.chengdongqing.wechat.features.contacts.data.mapper.toContact
import top.chengdongqing.wechat.features.contacts.data.mapper.toListItem
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.ContactItem
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class ContactPickerViewModel @Inject constructor(
    contactRepository: ContactRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactPickerUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * 组合多个数据流
     */
    val contactState = combine(
        contactRepository.observeAllContacts(),
        profileRepository.observeProfile()
    ) { contacts, myProfile ->
        // 将自己插入到联系人列表
        val allContacts = if (myProfile != null) {
            contacts + myProfile.toContact()
        } else {
            contacts
        }

        // 根据首字母分组
        val groups = allContacts.groupByInitial()
        // 计算索引映射
        val indexMap = calculateIndexMap(groups)

        _uiState.update {
            it.copy(isLoading = false)
        }

        Pair(groups, indexMap)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Pair(emptyMap(), emptyMap())
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

    // region 联系人选择

    fun isSelected(contactId: String): Boolean {
        return contactId in _uiState.value.selectedIds
    }

    fun toggleSelection(contactId: String) {
        _uiState.update {
            val newSet = if (contactId in it.selectedIds) {
                it.selectedIds - contactId
            } else {
                it.selectedIds + contactId
            }
            it.copy(selectedIds = newSet)
        }
    }

    // endregion
}

// UI State
data class ContactPickerUiState(
    val isLoading: Boolean = true,
    val selectedIds: Set<String> = emptySet(),
) {
    val selectedCount: Int
        get() = selectedIds.size
}