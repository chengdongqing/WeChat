package top.chengdongqing.wechat.features.settings.ui.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.settings.domain.model.AppFontScale
import top.chengdongqing.wechat.features.settings.domain.model.AppLanguage
import top.chengdongqing.wechat.features.settings.domain.model.AppTheme
import top.chengdongqing.wechat.features.settings.domain.model.DisplaySettings
import top.chengdongqing.wechat.features.settings.domain.repository.DisplaySettingsRepository
import javax.inject.Inject

@HiltViewModel
class DisplaySettingsViewModel @Inject constructor(
    private val repository: DisplaySettingsRepository
) : ViewModel() {

    val settings = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DisplaySettings()
        )

    val fontScale: StateFlow<AppFontScale> = settings
        .map { it.fontScale }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppFontScale.Normal)

    val theme: StateFlow<AppTheme> = settings
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppTheme.FollowSystem)

    val language: StateFlow<AppLanguage> = settings
        .map { it.language }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.FollowSystem)

    fun saveFontScale(fontScale: AppFontScale) {
        viewModelScope.launch { repository.saveFontScale(fontScale) }
    }

    fun saveTheme(theme: AppTheme) {
        viewModelScope.launch { repository.saveTheme(theme) }
    }

    fun saveLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.saveLanguage(language) }
    }
}