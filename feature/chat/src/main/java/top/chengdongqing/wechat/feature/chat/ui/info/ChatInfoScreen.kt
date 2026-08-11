package top.chengdongqing.wechat.feature.chat.ui.info

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.DashedAddButton
import top.chengdongqing.wechat.core.designsystem.components.dialog.DialogManager
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.SemanticError
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.feature.chat.ai.LocalAiModelInfo
import top.chengdongqing.wechat.feature.chat.ai.LocalAiState
import top.chengdongqing.wechat.feature.chat.ai.messageRes
import top.chengdongqing.wechat.feature.common.background.ChatBackgroundSetting
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun ChatInfoScreen(
    onBack: () -> Unit,
    onContact: () -> Unit,
    onRequestAddFriend: () -> Unit,
    onEndTemporaryChat: () -> Unit,
    viewModel: ChatInfoViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val selectAiModel = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::importLocalAiModel)
    }

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.chat_info),
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBouncedOverscrollEffect()
                )
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactListBar(
                name = uiState.contactName,
                avatarPath = uiState.contactAvatar,
                isAiAssistant = uiState.isAiAssistant,
                onContact = onContact
            )

            if (uiState.isAiAssistant) {
                LocalAiModelSettings(
                    state = uiState.localAiState,
                    modelSizeBytes = uiState.modelSizeBytes,
                    modelInfo = uiState.modelInfo,
                    onSelectModel = {
                        selectAiModel.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    onCancelLoading = viewModel::cancelModelLoading,
                    onUnloadModel = {
                        DialogManager.show(
                            title = resources.getString(R.string.chat_info_ai_unload_model_title),
                            content = resources.getString(R.string.chat_info_ai_unload_model_content),
                            okText = DesignR.string.action_ok,
                            okColor = SemanticError,
                            onOk = viewModel::unloadModel
                        )
                    }
                )
            }

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
                    label = stringResource(R.string.chat_info_bottom),
                    showArrow = false
                ) {
                    WeSwitch(checked = uiState.isBottomed) {
                        viewModel.toggleBottomed()
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
            if (uiState.isTemporary) {
                WeSettingGroup {
                    WeSettingItem(
                        label = stringResource(R.string.chat_info_temporary),
                        description = stringResource(R.string.chat_info_temporary_idle_description),
                        showArrow = false
                    ) {
                        uiState.expiresAt?.let {
                            WeSettingValue(formatTemporaryExpiry(it))
                        }
                    }
                    if (!uiState.isFriend) {
                        WeSettingItem(
                            label = stringResource(R.string.chat_info_promote_temporary),
                            description = stringResource(R.string.chat_info_promote_temporary_description),
                            onClick = onRequestAddFriend
                        )
                    }
                    WeSettingItem(
                        label = stringResource(R.string.chat_info_end_temporary),
                        showDivider = false,
                        onClick = {
                            DialogManager.show(
                                title = resources.getString(R.string.chat_info_end_temporary_title),
                                content = resources.getString(R.string.chat_info_end_temporary_content),
                                okText = DesignR.string.action_ok,
                                okColor = SemanticError,
                                onOk = { viewModel.endTemporaryChat(onEndTemporaryChat) }
                            )
                        }
                    )
                }
            }
            ChatBackgroundSetting(
                label = stringResource(R.string.chat_info_background_setting),
                value = uiState.backgroundPath,
            ) {
                viewModel.updateBackground(it)
            }
            WeSettingItem(
                label = stringResource(R.string.chat_info_clear),
                showDivider = false,
                onClick = {
                    DialogManager.show(
                        title = resources.getString(
                            R.string.chat_info_clear_title,
                            uiState.contactName
                        ),
                        okText = DesignR.string.action_clear,
                        okColor = SemanticError,
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

private fun formatTemporaryExpiry(timestamp: Long): String =
    java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(timestamp))

@Composable
private fun LocalAiModelSettings(
    state: LocalAiState,
    modelSizeBytes: Long?,
    modelInfo: LocalAiModelInfo?,
    onSelectModel: () -> Unit,
    onCancelLoading: () -> Unit,
    onUnloadModel: () -> Unit
) {
    val modelName = when (state) {
        is LocalAiState.Ready -> state.modelName
        else -> null
    }
    val status = when (state) {
        LocalAiState.NoModel -> stringResource(R.string.chat_info_ai_status_not_selected)
        is LocalAiState.Importing -> stringResource(R.string.chat_info_ai_status_importing, state.progressBytes / 1024 / 1024)
        LocalAiState.Loading -> stringResource(R.string.chat_info_ai_status_loading)
        LocalAiState.Cancelling -> stringResource(R.string.chat_info_ai_status_cancelling)
        is LocalAiState.Ready -> stringResource(R.string.chat_info_ai_status_loaded)
        is LocalAiState.Error -> stringResource(
            R.string.chat_info_ai_status_error,
            stringResource(state.error.messageRes)
        )
    }

    WeSettingGroup {
        WeSettingItem(stringResource(R.string.chat_info_ai_local_model), onClick = onSelectModel) {
            WeSettingValue(
                text = modelName ?: stringResource(R.string.chat_info_ai_select_model),
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }
        if (state !is LocalAiState.NoModel) {
            WeSettingItem(stringResource(R.string.chat_info_ai_model_status), showArrow = false) {
                WeSettingValue(status)
            }
            modelSizeBytes?.let { bytes ->
                WeSettingItem(stringResource(R.string.chat_info_ai_file_size), showArrow = false) {
                    WeSettingValue("%.2f GB".format(bytes / 1024.0 / 1024.0 / 1024.0))
                }
            }
            modelInfo?.description?.takeIf(String::isNotBlank)?.let { description ->
                WeSettingItem(
                    label = stringResource(R.string.chat_info_ai_model_description),
                    description = description,
                    showArrow = false
                )
            }
            modelInfo?.architecture?.let { architecture ->
                WeSettingItem(stringResource(R.string.chat_info_ai_model_architecture), showArrow = false) {
                    WeSettingValue(architecture)
                }
            }
            modelInfo?.parameterCount?.let { count ->
                WeSettingItem(stringResource(R.string.chat_info_ai_parameter_count), showArrow = false) {
                    WeSettingValue(formatParameterCount(count))
                }
            }
            modelInfo?.contextLength?.let { length ->
                WeSettingItem(stringResource(R.string.chat_info_ai_context_length), showArrow = false) {
                    WeSettingValue(stringResource(R.string.chat_info_ai_token_count, length))
                }
            }
            modelInfo?.fileType?.let { fileType ->
                WeSettingItem(stringResource(R.string.chat_info_ai_gguf_type), showArrow = false) {
                    WeSettingValue(ggufFileTypeLabel(fileType))
                }
            }
            when (state) {
                is LocalAiState.Importing, LocalAiState.Loading -> WeSettingItem(
                    label = stringResource(R.string.chat_info_ai_cancel_loading),
                    showDivider = false,
                    onClick = onCancelLoading
                )

                is LocalAiState.Ready -> WeSettingItem(
                    label = stringResource(R.string.chat_info_ai_unload_model),
                    showDivider = false,
                    onClick = onUnloadModel
                )

                else -> Unit
            }
        }
    }
}

private fun formatParameterCount(count: Long): String = when {
    count >= 1_000_000_000 -> "%.1fB".format(count / 1_000_000_000.0)
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    else -> count.toString()
}

@Composable
private fun ggufFileTypeLabel(type: Int): String = when (type) {
    0 -> "F32"
    1 -> "F16"
    2 -> "Q4_0"
    3 -> "Q4_1"
    7 -> "Q8_0"
    8 -> "Q5_0"
    9 -> "Q5_1"
    14 -> "Q6_K"
    15 -> "Q5_K_M"
    16 -> "Q4_K_M"
    17 -> "Q3_K_M"
    18 -> "Q2_K"
    else -> stringResource(R.string.chat_info_ai_unknown_gguf_type, type)
}

@Composable
private fun ContactListBar(
    name: String,
    avatarPath: String?,
    isAiAssistant: Boolean,
    onContact: () -> Unit
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
                .onTap { onContact() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = if (isAiAssistant) DesignR.drawable.img_logo else avatarPath,
                error = painterResource(DesignR.drawable.img_avatar_placeholder),
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

        if (!isAiAssistant) {
            DashedAddButton(
                modifier = Modifier.size(64.dp),
                cornerRadius = 6.dp,
                color = Color.Gray
            ) {}
        }
    }
}
