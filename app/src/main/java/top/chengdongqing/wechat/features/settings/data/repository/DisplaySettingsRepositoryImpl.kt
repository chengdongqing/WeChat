package top.chengdongqing.wechat.features.settings.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.di.DisplaySettingsDataStore
import top.chengdongqing.wechat.features.settings.domain.model.AppFontScale
import top.chengdongqing.wechat.features.settings.domain.model.AppLanguage
import top.chengdongqing.wechat.features.settings.domain.model.AppTheme
import top.chengdongqing.wechat.features.settings.domain.model.DisplaySettings
import top.chengdongqing.wechat.features.settings.domain.repository.DisplaySettingsRepository
import javax.inject.Inject

class DisplaySettingsRepositoryImpl @Inject constructor(
    @param:DisplaySettingsDataStore private val dataStore: DataStore<Preferences>
) : DisplaySettingsRepository {

    companion object {
        private val KEY_FONT_SCALE = stringPreferencesKey("app_font_scale")
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    }

    /** 读取所有显示设置，首次启动时语言自动匹配系统 */
    override val settings: Flow<DisplaySettings> =
        dataStore.data.map { prefs ->
            DisplaySettings(
                fontScale = AppFontScale.fromName(prefs[KEY_FONT_SCALE]),
                theme = AppTheme.fromName(prefs[KEY_THEME]),
                language = AppLanguage.fromName(prefs[KEY_LANGUAGE])
            )
        }

    override suspend fun saveFontScale(fontScale: AppFontScale) {
        dataStore.edit { it[KEY_FONT_SCALE] = fontScale.name }
    }

    override suspend fun saveTheme(theme: AppTheme) {
        dataStore.edit { it[KEY_THEME] = theme.name }
        // 立即应用深色模式
        AppCompatDelegate.setDefaultNightMode(theme.mode)
    }

    override suspend fun saveLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_LANGUAGE] = language.name }
        // 立即应用语言
        val localeList = when (language) {
            AppLanguage.FollowSystem -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(language.locale)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    override suspend fun restoreOnStartup() {
        val prefs = dataStore.data.first()

        AppCompatDelegate.setDefaultNightMode(
            AppTheme.fromName(prefs[KEY_THEME]).mode
        )

        val localeList = when (val language = AppLanguage.fromName(prefs[KEY_LANGUAGE])) {
            AppLanguage.FollowSystem -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(language.locale)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}