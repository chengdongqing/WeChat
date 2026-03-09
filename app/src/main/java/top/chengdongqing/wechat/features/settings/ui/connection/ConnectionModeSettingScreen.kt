package top.chengdongqing.wechat.features.settings.ui.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.settings.domain.model.ConnectionMode

@Composable
fun ConnectionModeSettingScreen(onBack: () -> Unit) {
    val resources = LocalResources.current
    val connectionOptions = remember {
        ConnectionMode.entries.map {
            resources.getString(it.labelRes) to it
        }
    }
    var connection by remember { mutableStateOf(ConnectionMode.WifiLan) }

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.settings_connection),
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
                options = connectionOptions,
                value = connection
            ) {
                connection = it
            }

            SettingHint()
        }
    }
}

@Composable
private fun SettingHint() {
    val textStyle = TextStyle(
        color = WeTheme.colorScheme.textSecondary,
        fontSize = 13.sp
    )

    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = stringResource(R.string.connection_hint),
            style = textStyle
        )
        Spacer(modifier = Modifier.height(26.dp))
        ConnectionMode.entries.forEachIndexed { index, mode ->
            Text(
                text = "${index + 1}. ${stringResource(mode.labelRes)}: ${stringResource(mode.descriptionRes)}",
                style = textStyle
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}