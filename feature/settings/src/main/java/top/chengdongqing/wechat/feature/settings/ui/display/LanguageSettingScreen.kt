package top.chengdongqing.wechat.feature.settings.ui.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.settings.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.ui.labelRes
import top.chengdongqing.wechat.core.model.AppLanguage

@Composable
fun LanguageSettingScreen(
    onBack: () -> Unit,
    viewModel: DisplaySettingsViewModel = hiltViewModel()
) {
    val resources = LocalResources.current
    val initialLanguage by viewModel.language.collectAsStateWithLifecycle()
    var language by remember(initialLanguage) { mutableStateOf(initialLanguage) }
    val languageOptions = remember {
        AppLanguage.entries.map {
            resources.getString(it.labelRes) to it
        }
    }
    val hasChanged = language != initialLanguage

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.display_language),
                onBack = onBack
            ) {
                WeButton(
                    text = stringResource(DesignR.string.action_done),
                    size = ButtonSize.Small,
                    enabled = hasChanged
                ) {
                    viewModel.saveLanguage(language)
                    onBack()
                }
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WeRadioGroup(
                options = languageOptions,
                value = language
            ) {
                language = it
            }
        }
    }
}
