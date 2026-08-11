package top.chengdongqing.wechat.feature.settings.ui.notification

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
import top.chengdongqing.wechat.core.notification.NotificationDisplay

@Composable
fun NotificationDisplaySettingScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val resources = LocalResources.current
    val initialDisplay by viewModel.notificationDisplay.collectAsStateWithLifecycle()
    var display by remember(initialDisplay) { mutableStateOf(initialDisplay) }
    val displayOptions = remember {
        NotificationDisplay.entries.map {
            resources.getString(it.descriptionRes) to it
        }
    }
    val hasChanged = display != initialDisplay

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.notification_display),
                onBack = onBack
            ) {
                WeButton(
                    text = stringResource(DesignR.string.action_done),
                    size = ButtonSize.Small,
                    enabled = hasChanged
                ) {
                    viewModel.setNotificationDisplay(display)
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
                options = displayOptions,
                value = display
            ) {
                display = it
            }
        }
    }
}
