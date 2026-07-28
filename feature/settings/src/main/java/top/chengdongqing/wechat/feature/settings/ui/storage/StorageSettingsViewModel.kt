package top.chengdongqing.wechat.feature.settings.ui.storage

import android.content.Context
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.database.dao.MediaFileDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import java.io.File
import javax.inject.Inject

data class StorageUiState(
    val loading: Boolean = true,
    val totalBytes: Long = 0,
    val freeBytes: Long = 0,
    val appBytes: Long = 0,
    val cacheBytes: Long = 0,
    val chatBytes: Long = 0,
    val resourceBytes: Long = 0,
    val necessaryBytes: Long = 0,
    val cleaning: StorageCategory? = null
)

enum class StorageCategory { Cache, Chats, Resources }

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
    private val mediaFileDao: MediaFileDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = calculate().copy(loading = false)
        }
    }

    fun clean(category: StorageCategory) {
        if (_uiState.value.cleaning != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cleaning = category)
            withContext(Dispatchers.IO) {
                when (category) {
                    StorageCategory.Cache -> {
                        context.cacheDir.deleteChildren()
                        context.codeCacheDir.deleteChildren()
                    }
                    StorageCategory.Chats -> {
                        messageDao.getAllLocalPaths().forEach { File(it).delete() }
                        messageDao.clearAllLocalPaths()
                        mediaFileDao.getUnreferencedPaths().forEach { File(it).delete() }
                        mediaFileDao.deleteUnreferenced()
                    }
                    StorageCategory.Resources -> RESOURCE_DIRS.forEach {
                        File(context.filesDir, it).deleteChildren()
                    }
                }
            }
            _uiState.value = calculate().copy(loading = false)
        }
    }

    private suspend fun calculate(): StorageUiState = withContext(Dispatchers.IO) {
        val stats = StatFs(context.filesDir.absolutePath)
        val total = stats.totalBytes
        val free = stats.availableBytes
        val cache = context.cacheDir.sizeRecursively() + context.codeCacheDir.sizeRecursively()
        val chatPaths = messageDao.getAllLocalPaths().distinct().map(::File)
        val chat = chatPaths.sumOf { it.sizeRecursively() }
        val resources = RESOURCE_DIRS.sumOf { File(context.filesDir, it).sizeRecursively() }
        val files = context.filesDir.sizeRecursively()
        val databases = File(context.applicationInfo.dataDir, "databases").sizeRecursively()
        val preferences = File(context.applicationInfo.dataDir, "shared_prefs").sizeRecursively()
        val apk = File(context.applicationInfo.sourceDir).length()
        val necessary = (files - chat - resources).coerceAtLeast(0) + databases + preferences + apk
        StorageUiState(
            loading = false,
            totalBytes = total,
            freeBytes = free,
            appBytes = cache + chat + resources + necessary,
            cacheBytes = cache,
            chatBytes = chat,
            resourceBytes = resources,
            necessaryBytes = necessary
        )
    }

    private fun File.sizeRecursively(): Long = when {
        !exists() -> 0
        isFile -> length()
        else -> listFiles()?.sumOf { it.sizeRecursively() } ?: 0
    }

    private fun File.deleteChildren() {
        if (!exists() || !isDirectory) return
        listFiles()?.forEach { it.deleteRecursively() }
    }

    private companion object {
        val RESOURCE_DIRS = listOf("stickers", "music")
    }
}
