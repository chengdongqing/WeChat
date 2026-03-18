package top.chengdongqing.wechat.core.designsystem.components.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.designsystem.components.app.model.AppItem
import top.chengdongqing.wechat.core.designsystem.components.app.repository.AppRepository
import top.chengdongqing.wechat.core.util.getInitial
import javax.inject.Inject

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPickerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadApps()
    }

    // ─────────────────────────────────────────────────────────
    // 数据加载
    // ─────────────────────────────────────────────────────────

    private fun loadApps() {
        viewModelScope.launch {
            runCatching { appRepository.loadInstalledApks() }
                .onSuccess { apks ->
                    val groups = apks.groupByInitial()
                    val indexMap = calculateIndexMap(groups)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            apks = apks,
                            groups = groups,
                            indexMap = indexMap
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 分组逻辑
    // ─────────────────────────────────────────────────────────

    /**
     * 按应用名首字母分组并排序，非字母开头归入 '#' 组排最后
     */
    private suspend fun List<AppItem>.groupByInitial(): Map<Char, List<AppItem>> =
        withContext(Dispatchers.Default) {
            groupBy { it.name.getInitial() }
                .toSortedMap { a, b ->
                    when {
                        a == '#' -> 1
                        b == '#' -> -1
                        else -> a.compareTo(b)
                    }
                }
        }

    /**
     * 计算每个首字母在列表中对应的起始索引，用于快速定位侧边栏
     */
    private fun calculateIndexMap(groups: Map<Char, List<AppItem>>): Map<Char, Int> {
        var currentIndex = 1 // 顶部有一个空白间距
        return buildMap {
            groups.forEach { (initial, apks) ->
                put(initial, currentIndex)
                // 每组消耗：1 (Header) + N (ApkItem)
                currentIndex += apks.size + 1
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 选择逻辑
    // ─────────────────────────────────────────────────────────

    fun isSelected(apk: AppItem): Boolean =
        apk in _uiState.value.selectedApps

    fun toggleSelection(apk: AppItem) {
        _uiState.update { state ->
            val updated = if (apk in state.selectedApps) {
                state.selectedApps - apk
            } else {
                state.selectedApps + apk
            }
            state.copy(selectedApps = updated)
        }
    }
}

data class AppPickerUiState(
    val isLoading: Boolean = true,
    val apks: List<AppItem> = emptyList(),
    val groups: Map<Char, List<AppItem>> = emptyMap(),
    val indexMap: Map<Char, Int> = emptyMap(),
    val selectedApps: List<AppItem> = emptyList(),
    val error: String? = null,
) {
    val selectedCount: Int
        get() = selectedApps.size
}