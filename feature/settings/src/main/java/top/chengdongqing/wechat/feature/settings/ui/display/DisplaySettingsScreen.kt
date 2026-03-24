package top.chengdongqing.wechat.feature.settings.ui.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.core.common.navigation.SettingsRoute
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun DisplaySettingsScreen(
    navController: NavHostController,
    onBack: () -> Unit,
    viewModel: DisplaySettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.settings_display),
                onBack = onBack
            )
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
                label = stringResource(R.string.display_theme),
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.ThemeSetting.route)
                }
            ) {
                WeSettingValue(stringResource(settings.theme.labelRes))
            }
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.display_font_scale),
                    onClick = {
                        navController.navigate(SettingsRoute.FontScaleSetting.route)
                    }
                )
                WeSettingItem(
                    label = stringResource(R.string.display_language),
                    showDivider = false,
                    onClick = {
                        navController.navigate(SettingsRoute.LanguageSetting.route)
                    }
                ) {
                    WeSettingValue(stringResource(settings.language.labelRes))
                }
            }
        }
    }
}