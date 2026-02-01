package top.chengdongqing.wechat.ui.contacts.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.PinyinUtils.getInitial
import top.chengdongqing.wechat.core.utils.randomUUID
import javax.inject.Inject

data class ContactListState(
    val isLoading: Boolean = true,
    val groups: Map<Char, List<Contact>> = emptyMap(),
    val totalCount: Int = 0,
    val indexMap: Map<Char, Int> = emptyMap() // 索引表：保存预计算的索引位置
)

data class Contact(
    val id: String,
    val name: String,
    val avatar: Int,
    val initial: Char // 首字母
)

@HiltViewModel
class ContactListViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(ContactListState())
    val state: StateFlow<ContactListState> = _state.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val contacts = generateMockContacts(500)
            val groups = contacts.groupByInitial()
            val indexMap = calculateIndexMap(groups)

            _state.update {
                it.copy(
                    isLoading = false,
                    groups = groups,
                    totalCount = contacts.size,
                    indexMap = indexMap
                )
            }
        }
    }

    private suspend fun generateMockContacts(count: Int): List<Contact> =
        withContext(Dispatchers.Default) {
            val surnames = listOf(
                "阿",
                "巴",
                "陈",
                "戴",
                "鄂",
                "付",
                "高",
                "何",
                "金",
                "李",
                "马",
                "牛",
                "彭",
                "秦",
                "苏",
                "万",
                "夏",
                "张",
                "~",
                "*"
            )
            val names = listOf("强", "玲", "伟", "芳", "杰", "秀", "涛", "娜", "军", "明")

            val chunkSize = (count / Runtime.getRuntime().availableProcessors()).coerceAtLeast(1)

            (0 until count step chunkSize).map { start ->
                async {
                    val end = (start + chunkSize).coerceAtMost(count)
                    List(end - start) {
                        val name = "${surnames.random()}${names.random()}"
                        Contact(
                            id = randomUUID(),
                            name = name,
                            avatar = R.drawable.img_logo,
                            initial = name.getInitial()
                        )
                    }
                }
            }.awaitAll().flatten()
        }

    private fun List<Contact>.groupByInitial(): Map<Char, List<Contact>> =
        this.groupBy { it.initial }.toSortedMap { a, b ->
            when {
                a == '#' -> 1
                b == '#' -> -1
                else -> a.compareTo(b)
            }
        }

    private fun calculateIndexMap(groups: Map<Char, List<Contact>>): Map<Char, Int> {
        val indexMap = mutableMapOf<Char, Int>()
        // 顶部固定功能项占了 1 个 item 位置
        var currentIndex = 1

        groups.forEach { (initial, contacts) ->
            indexMap[initial] = currentIndex
            // 每一个分组消耗：1 (Header) + N (Contacts)
            currentIndex += (contacts.size + 1)
        }
        return indexMap
    }
}