package top.chengdongqing.wechat.feature.profile.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.data.repository.MessageRepository
import top.chengdongqing.wechat.core.database.dao.FavoriteDao
import top.chengdongqing.wechat.core.database.entity.FavoriteEntity
import top.chengdongqing.wechat.core.util.randomUUID
import java.io.File
import javax.inject.Inject

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val messageRepository: MessageRepository
) : ViewModel() {
    val query = MutableStateFlow("")
    val selected = MutableStateFlow<Set<String>>(emptySet())
    val selectionMode = MutableStateFlow(false)
    val selectedType = MutableStateFlow("")
    val favorites = combine(query.debounce(250), selectedType) { query, type -> query to type }
        .flatMapLatest { (value, type) ->
            Pager(PagingConfig(pageSize = 20, prefetchDistance = 5)) {
                favoriteDao.pagingSource(value.trim(), type)
            }.flow
        }.cachedIn(viewModelScope)

    fun toggle(id: String) {
        selected.value = selected.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun enterSelectionMode() {
        selectionMode.value = true
    }

    fun clearSelection() {
        selected.value = emptySet()
        selectionMode.value = false
    }

    fun deleteSelected() = viewModelScope.launch {
        favoriteDao.delete(selected.value)
        clearSelection()
    }

    fun forwardSelected(targetChatId: String, onComplete: () -> Unit) = viewModelScope.launch {
        val items = favoriteDao.getByIds(selected.value)
        items.forEach { item ->
            messageRepository.sendMessage(
                sessionId = targetChatId,
                receiverId = targetChatId,
                messageId = randomUUID(),
                content = item.toMessageContent()
            )
        }
        clearSelection()
        onComplete()
    }

    fun forwardSelected(targetChatIds: Set<String>) = viewModelScope.launch {
        val items = favoriteDao.getByIds(selected.value)
        targetChatIds.forEach { chatId ->
            items.forEach { item ->
                messageRepository.sendMessage(
                    sessionId = chatId,
                    receiverId = chatId,
                    messageId = randomUUID(),
                    content = item.toMessageContent()
                )
            }
        }
        clearSelection()
    }
}

private fun FavoriteEntity.toMessageContent(): MessageContent {
    val attachment = decodeFavoriteAttachments(mediaPaths).firstOrNull()
    return when (attachment?.kind) {
        FavoriteAttachment.Kind.AUDIO ->
            MessageContent.Voice(attachment.path, attachment.durationMs)

        FavoriteAttachment.Kind.LOCATION -> {
            MessageContent.Location(
                attachment.latitude,
                attachment.longitude,
                attachment.address,
                attachment.locationName,
                attachment.mapUri.takeIf(String::isNotBlank)
            )
        }

        FavoriteAttachment.Kind.IMAGE, FavoriteAttachment.Kind.VIDEO,
        FavoriteAttachment.Kind.FILE -> {
            val file = File(attachment.path)
            MessageContent.File(
                attachment.path,
                attachment.displayName.ifBlank { file.name },
                attachment.mimeType.ifBlank { "application/octet-stream" },
                file.length()
            )
        }

        null -> MessageContent.Text(listOf(title, content).filter { it.isNotBlank() }
            .joinToString("\n"))
    }
}
