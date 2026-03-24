package top.chengdongqing.wechat.feature.settings.ui.chat

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
import top.chengdongqing.wechat.core.common.background.ChatBackgroundSetting
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun ChatSettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatSettingsViewModel = hiltViewModel()
) {
    val speakerEnabled by viewModel.speakerEnabled.collectAsStateWithLifecycle()
    val sendButtonEnabled by viewModel.sendButtonEnabled.collectAsStateWithLifecycle()
    val e2eEnabled by viewModel.e2eEnabled.collectAsStateWithLifecycle()
    val chatBackground by viewModel.chatBackground.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.settings_chat),
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
            ChatBackgroundSetting(
                label = stringResource(R.string.chat_settings_background),
                value = chatBackground,
            ) {
                viewModel.setChatBackground(it)
            }
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.chat_settings_earpiece),
                    showArrow = false
                ) {
                    WeSwitch(
                        checked = !speakerEnabled,
                        onChange = viewModel::toggleSpeaker
                    )
                }
                WeSettingItem(
                    label = stringResource(R.string.chat_settings_send_button),
                    showArrow = false,
                    showDivider = false,
                ) {
                    WeSwitch(
                        checked = sendButtonEnabled,
                        onChange = viewModel::toggleSendButton
                    )
                }
            }
            WeSettingItem(
                label = stringResource(R.string.chat_settings_e2e),
                description = stringResource(R.string.chat_settings_e2e_desc),
                showArrow = false,
                showDivider = false,
                height = 68.dp
            ) {
                WeSwitch(
                    checked = e2eEnabled,
                    onChange = viewModel::toggleE2e
                )
            }
        }
    }
}