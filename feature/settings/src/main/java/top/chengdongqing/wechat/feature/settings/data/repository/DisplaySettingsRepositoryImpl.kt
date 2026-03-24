package top.chengdongqing.wechat.feature.settings.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.common.di.DisplaySettingsDataStore
import top.chengdongqing.wechat.core.data.repository.DisplaySettingsRepository
import top.chengdongqing.wechat.core.model.AppFontScale
import top.chengdongqing.wechat.core.model.AppLanguage
import top.chengdongqing.wechat.core.model.AppTheme
import top.chengdongqing.wechat.core.model.DisplaySettings
import javax.inject.Inject

class DisplaySettingsRepositoryImpl @Inject constructor(
    @param:DisplaySettingsDataStore private val dataStore: DataStore<Preferences>
) : DisplaySettingsRepository {

    companion object {
        private val KEY_FONT_SCALE = stringPreferencesKey("app_font_scale")
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    }

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

        // 应用：触发app资源重定向与activity重启
        AppCompatDelegate.setDefaultNightMode(theme.mode)
    }

    override suspend fun saveLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_LANGUAGE] = language.name }

        // 转换 Locale
        val locales = if (language == AppLanguage.FollowSystem) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.locale)
        }

        // 应用：触发app资源重定向与activity重启
        AppCompatDelegate.setApplicationLocales(locales)
    }
}