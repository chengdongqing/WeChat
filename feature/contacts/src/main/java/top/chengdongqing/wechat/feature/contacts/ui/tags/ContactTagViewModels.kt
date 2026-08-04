package top.chengdongqing.wechat.feature.contacts.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.database.dao.ContactDao
import top.chengdongqing.wechat.core.database.dao.ContactTagDao
import top.chengdongqing.wechat.core.database.dao.ContactTagSummary
import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.database.entity.ContactTagEntity
import top.chengdongqing.wechat.core.util.randomUUID
import javax.inject.Inject

@HiltViewModel
class ContactTagsViewModel @Inject constructor(tagDao: ContactTagDao) : ViewModel() {
    val tags = tagDao.observeTags().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
}

data class TagEditorState(
    val name: String = "",
    val contacts: List<ContactEntity> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val saving: Boolean = false
)

sealed interface TagEditorEvent {
    data object Saved : TagEditorEvent
    data object Deleted : TagEditorEvent
    data class Error(val message: String) : TagEditorEvent
}

@HiltViewModel(assistedFactory = ContactTagEditorViewModel.Factory::class)
class ContactTagEditorViewModel @AssistedInject constructor(
    @Assisted private val tagId: String?,
    private val tagDao: ContactTagDao,
    contactDao: ContactDao
) : ViewModel() {
    @AssistedFactory interface Factory {
        fun create(tagId: String?): ContactTagEditorViewModel
    }

    private val _state = MutableStateFlow(TagEditorState())
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<TagEditorEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val existingName = tagId?.let { tagDao.getTag(it)?.name }.orEmpty()
            combine(
                contactDao.observeAll(false),
                tagId?.let(tagDao::observeContacts) ?: flowOf(emptyList())
            ) { contacts, members -> contacts to members.map { it.id }.toSet() }
                .collect { (contacts, selected) ->
                    _state.update {
                        it.copy(
                            name = if (it.name.isBlank()) existingName else it.name,
                            contacts = contacts,
                            selectedIds = if (it.contacts.isEmpty()) selected else it.selectedIds
                        )
                    }
                }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value.take(32)) }
    fun toggle(contactId: String) = _state.update {
        it.copy(
            selectedIds = if (contactId in it.selectedIds) it.selectedIds - contactId
            else it.selectedIds + contactId
        )
    }

    fun save() {
        val name = state.value.name.trim()
        if (name.isBlank()) {
            viewModelScope.launch { _events.emit(TagEditorEvent.Error("标签名字不能为空")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            runCatching {
                val id = tagId ?: randomUUID()
                if (tagId == null) tagDao.insertTag(ContactTagEntity(id, name))
                else tagDao.rename(id, name)
                tagDao.replaceMembers(id, state.value.selectedIds)
            }.onSuccess {
                _events.emit(TagEditorEvent.Saved)
            }.onFailure {
                _events.emit(TagEditorEvent.Error("标签名称已存在或保存失败"))
            }
            _state.update { it.copy(saving = false) }
        }
    }

    fun delete() {
        val id = tagId ?: return
        viewModelScope.launch {
            tagDao.delete(id)
            _events.emit(TagEditorEvent.Deleted)
        }
    }
}

data class ContactTagPickerState(
    val tags: List<ContactTagSummary> = emptyList(),
    val selectedIds: Set<String> = emptySet()
)

@HiltViewModel(assistedFactory = ContactTagPickerViewModel.Factory::class)
class ContactTagPickerViewModel @AssistedInject constructor(
    @Assisted private val contactId: String,
    private val tagDao: ContactTagDao
) : ViewModel() {
    @AssistedFactory interface Factory {
        fun create(contactId: String): ContactTagPickerViewModel
    }

    private val _state = MutableStateFlow(ContactTagPickerState())
    val state = _state.asStateFlow()
    private var touched = false

    init {
        viewModelScope.launch {
            combine(tagDao.observeTags(), tagDao.observeTagIds(contactId)) { tags, ids ->
                tags to ids.toSet()
            }.collect { (tags, ids) ->
                _state.update { it.copy(tags = tags, selectedIds = if (touched) it.selectedIds else ids) }
            }
        }
    }

    fun toggle(tagId: String) {
        touched = true
        _state.update {
            it.copy(selectedIds = if (tagId in it.selectedIds) it.selectedIds - tagId else it.selectedIds + tagId)
        }
    }

    suspend fun save() = tagDao.replaceContactTags(contactId, state.value.selectedIds)
}
