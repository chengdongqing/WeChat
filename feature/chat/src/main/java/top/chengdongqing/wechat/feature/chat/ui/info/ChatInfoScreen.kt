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
import top.chengdongqing.wechat.core.common.background.ChatBackgroundSetting
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.DashedAddButton
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.SemanticError
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.chat.ai.LocalAiModelInfo
import top.chengdongqing.wechat.feature.chat.ai.LocalAiState

@Composable
fun ChatInfoScreen(
    onBack: () -> Unit,
    onNavigateToContact: () -> Unit,
    viewModel: ChatInfoViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val dialog = rememberDialogState()
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactListBar(
                name = uiState.contactName,
                avatarPath = uiState.contactAvatar,
                isLocalAi = uiState.isLocalAi,
                onNavigateToContact = onNavigateToContact
            )

            if (uiState.isLocalAi) {
                LocalAiModelSettings(
                    state = uiState.localAiState,
                    modelSizeBytes = uiState.modelSizeBytes,
                    modelInfo = uiState.modelInfo,
                    onSelectModel = {
                        selectAiModel.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    onCancelLoading = viewModel::cancelModelLoading,
                    onUnloadModel = {
                        dialog.show(
                            title = "确定卸载模型吗？",
                            content = "下次使用时可重新导入。",
                            okText = R.string.action_ok,
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
                    label = stringResource(R.string.chat_info_remind),
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch()
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
                    dialog.show(
                        title = resources.getString(
                            R.string.chat_info_clear_title,
                            uiState.contactName
                        ),
                        okText = R.string.action_clear,
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
        LocalAiState.NoModel -> "未选择"
        is LocalAiState.Importing -> "正在导入 ${(state.progressBytes / 1024 / 1024)} MB"
        LocalAiState.Loading -> "正在加载"
        LocalAiState.Cancelling -> "正在取消加载"
        is LocalAiState.Ready -> "已加载"
        is LocalAiState.Error -> "加载失败：${state.message}"
    }

    WeSettingGroup {
        WeSettingItem("本地模型", onClick = onSelectModel) {
            WeSettingValue(
                text = modelName ?: "选择 GGUF 模型",
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }
        if (state !is LocalAiState.NoModel) {
            WeSettingItem("模型状态", showArrow = false) {
                WeSettingValue(status)
            }
            modelSizeBytes?.let { bytes ->
                WeSettingItem("文件大小", showArrow = false) {
                    WeSettingValue("%.2f GB".format(bytes / 1024.0 / 1024.0 / 1024.0))
                }
            }
            modelInfo?.description?.takeIf(String::isNotBlank)?.let { description ->
                WeSettingItem(
                    label = "模型描述",
                    description = description,
                    showArrow = false
                )
            }
            modelInfo?.architecture?.let { architecture ->
                WeSettingItem("模型架构", showArrow = false) {
                    WeSettingValue(architecture)
                }
            }
            modelInfo?.parameterCount?.let { count ->
                WeSettingItem("参数量", showArrow = false) {
                    WeSettingValue(formatParameterCount(count))
                }
            }
            modelInfo?.contextLength?.let { length ->
                WeSettingItem("上下文长度", showArrow = false) {
                    WeSettingValue("$length tokens")
                }
            }
            modelInfo?.fileType?.let { fileType ->
                WeSettingItem("GGUF 类型", showArrow = false) {
                    WeSettingValue(ggufFileTypeLabel(fileType))
                }
            }
            when (state) {
                is LocalAiState.Importing, LocalAiState.Loading -> WeSettingItem(
                    label = "取消加载",
                    showDivider = false,
                    onClick = onCancelLoading
                )

                is LocalAiState.Ready -> WeSettingItem(
                    label = "卸载模型",
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
    else -> "类型 $type"
}

@Composable
private fun ContactListBar(
    name: String,
    avatarPath: String?,
    isLocalAi: Boolean,
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
                .onTap { onNavigateToContact() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = if (isLocalAi) R.drawable.img_logo else avatarPath,
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

        if (!isLocalAi) {
            DashedAddButton(
                modifier = Modifier.size(64.dp),
                cornerRadius = 6.dp,
                color = Color.Gray
            ) {}
        }
    }
}
