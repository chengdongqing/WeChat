package top.chengdongqing.wechat.feature.profile.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.database.dao.FavoriteDao
import top.chengdongqing.wechat.core.database.entity.FavoriteEntity
import top.chengdongqing.wechat.core.util.randomUUID
import javax.inject.Inject

data class FavoriteDraft(
    val id: String? = randomUUID(),
    val type: String = "RICH_TEXT",
    val title: String = "",
    val content: String = "",
    val mediaPaths: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@HiltViewModel
class FavoriteEditorViewModel @Inject constructor(
    private val dao: FavoriteDao
) : ViewModel() {
    private val _draft = MutableStateFlow(FavoriteDraft())
    val draft = _draft.asStateFlow()
    private var loadedId: String? = null
    private var autoSaveJob: Job? = null

    fun load(id: String?) {
        if (id == null || loadedId == id) return
        loadedId = id
        viewModelScope.launch {
            dao.observe(id).collect { item ->
                if (item != null) _draft.value = FavoriteDraft(
                    item.id, item.type, item.title, item.content, item.mediaPaths, item.createdAt
                )
            }
        }
    }

    fun update(transform: (FavoriteDraft) -> FavoriteDraft) {
        _draft.value = transform(_draft.value)
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(600)
            persist()
        }
    }

    fun save(onComplete: () -> Unit) = viewModelScope.launch {
        autoSaveJob?.cancel()
        persist()
        onComplete()
    }

    fun delete(onComplete: () -> Unit) = viewModelScope.launch {
        autoSaveJob?.cancel()
        _draft.value.id?.let { dao.delete(setOf(it)) }
        onComplete()
    }

    private suspend fun persist() {
        val value = _draft.value
        if (value.title.isBlank() && value.content.isBlank() && value.mediaPaths.isBlank()) return
        val now = System.currentTimeMillis()
        dao.upsert(
            FavoriteEntity(
                id = value.id ?: randomUUID(),
                type = value.type,
                title = value.title.trim(),
                content = value.content.trim(),
                mediaPaths = value.mediaPaths.trim(),
                createdAt = value.createdAt,
                updatedAt = now
            )
        )
    }
}
