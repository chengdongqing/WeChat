package top.chengdongqing.wechat.features.settings.ui.notification

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
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.settings.domain.model.NotificationDisplay

@Composable
fun NotificationDisplaySettingScreen(onBack: () -> Unit) {
    val resources = LocalResources.current
    val displayOptions = remember {
        NotificationDisplay.entries.map {
            resources.getString(it.description) to it
        }
    }
    var display by remember { mutableStateOf(NotificationDisplay.SenderAndContent) }

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.notification_display),
                onBack = onBack
            ) {
                WeButton(
                    text = stringResource(R.string.action_done),
                    size = ButtonSize.Small,
                    enabled = false
                )
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