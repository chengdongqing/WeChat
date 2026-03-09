package top.chengdongqing.wechat.features.settings.ui.display

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.settings.domain.model.AppTheme

@Composable
fun DarkModeSettingScreen(
    onBack: () -> Unit,
    viewModel: DisplaySettingsViewModel = hiltViewModel()
) {
    val initialTheme by viewModel.theme.collectAsStateWithLifecycle()
    val resources = LocalResources.current

    var theme by remember(initialTheme) { mutableStateOf(initialTheme) }
    val themeOptions = remember {
        AppTheme.entries.filter {
            !it.isFollowSystem
        }.map {
            resources.getString(it.labelRes) to it
        }
    }
    val hasChanged = theme != initialTheme

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.display_theme),
                onBack = onBack
            ) {
                WeButton(
                    text = stringResource(R.string.action_done),
                    size = ButtonSize.Small,
                    enabled = hasChanged
                ) {
                    viewModel.saveTheme(theme)
                    onBack()
                }
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeSettingItem(
                label = stringResource(R.string.settings_follow_system),
                description = stringResource(R.string.display_theme_follow_system_desc),
                showArrow = false,
                showDivider = false,
                height = 68.dp
            ) {
                WeSwitch(checked = theme.isFollowSystem) { checked ->
                    theme = if (checked) AppTheme.FollowSystem else AppTheme.Light
                }
            }

            if (!theme.isFollowSystem) {
                WeSettingGroup(stringResource(R.string.display_theme_manual_select)) {
                    WeRadioGroup(
                        options = themeOptions,
                        value = theme
                    ) {
                        theme = it
                    }
                }
            }
        }
    }
}