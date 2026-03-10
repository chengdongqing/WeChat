package top.chengdongqing.wechat.features.chat.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.DashedAddButton
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.features.chat.ui.info.components.ChatBackgroundSetting

@Composable
fun ChatInfoScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToContact: (chatId: String) -> Unit,
    viewModel: ChatInfoViewModel = hiltViewModel { factory: ChatInfoViewModel.Factory ->
        factory.create(chatId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val dialog = rememberDialogState()

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.chat_info),
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactListBar(
                name = uiState.contactName,
                avatarPath = uiState.contactAvatar,
                onNavigateToContact = { onNavigateToContact(chatId) }
            )

            WeSettingItem(
                label = stringResource(R.string.chat_info_search),
                showDivider = false
            )
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.chat_info_mute),
                    showArrow = false
                ) {
                    WeSwitch(checked = uiState.isMuted) {
                        viewModel.toggleMuted()
                    }
                }
                WeSettingItem(
                    label = stringResource(R.string.chat_info_pin),
                    showArrow = false
                ) {
                    WeSwitch(checked = uiState.isPinned) {
                        viewModel.togglePinned()
                    }
                }
                WeSettingItem(
                    label = stringResource(R.string.chat_info_remind),
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch()
                }
            }
            ChatBackgroundSetting(background = uiState.backgroundPath) {
                viewModel.updateBackground(it)
            }
            WeSettingItem(
                label = stringResource(R.string.chat_info_clear),
                showDivider = false,
                onClick = {
                    dialog.show(
                        title = resources.getString(
                            R.string.chat_info_clear_title,
                            uiState.contactName
                        ),
                        okText = R.string.action_clear,
                        okColor = Danger,
                        onOk = { viewModel.clearMessages() }
                    )
                }
            )
            WeSettingItem(
                label = stringResource(R.string.chat_info_complaint),
                showDivider = false
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ContactListBar(
    name: String,
    avatarPath: String?,
    onNavigateToContact: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 80.dp)
                .weClickable { onNavigateToContact() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = avatarPath,
                error = painterResource(R.drawable.img_avatar_placeholder),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                fontSize = 13.sp,
                color = WeTheme.colorScheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DashedAddButton(
            modifier = Modifier.size(64.dp),
            cornerRadius = 6.dp,
            color = Color.Gray
        ) {}
    }
}