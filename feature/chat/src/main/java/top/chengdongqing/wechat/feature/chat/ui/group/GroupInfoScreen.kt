package top.chengdongqing.wechat.feature.chat.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.common.background.ChatBackgroundSetting
import top.chengdongqing.wechat.core.common.qrcode.QRCodeFormat
import top.chengdongqing.wechat.core.common.qrcode.generator.WeQRCode
import top.chengdongqing.wechat.core.common.qrcode.generator.rememberQRCodeState
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.menu.WeDangerButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.LocalContactPickerLauncher
import top.chengdongqing.wechat.feature.chat.ui.group.components.ChatParticipant
import top.chengdongqing.wechat.feature.chat.ui.group.components.ChatParticipantsBar

@Composable
fun GroupInfoScreen(
    onBack: () -> Unit,
    onExitGroup: () -> Unit,
    viewModel: GroupInfoViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<GroupEditor?>(null) }
    var editText by remember { mutableStateOf("") }
    var showRemoveMembers by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
    val pickMembers = LocalContactPickerLauncher.current.rememberLauncher(
        excludeSelf = true,
        onResult = viewModel::addMembers
    )
    val qrCodeState = rememberQRCodeState(
        QRCodeFormat.generateJoinGroupQRCode(viewModel.groupId)
    )

    fun openEditor(type: GroupEditor, value: String) {
        editor = type
        editText = value
    }

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = "聊天信息(${state.memberCount})",
                onBack = onBack,
                actions = {
                    IconButton(
                        icon = R.drawable.ic_search_outlined,
                        description = "搜索",
                        onClick = {}
                    )
                }
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatParticipantsBar(
                participants = state.members.map {
                    ChatParticipant(it.id, it.name, it.avatarPath)
                },
                onAdd = { pickMembers(200) },
                onManage = if (state.canManageMembers) {
                    ({ showRemoveMembers = true })
                } else null
            )

            WeSettingGroup {
                WeSettingItem("群聊名称", onClick = {
                    openEditor(GroupEditor.Name, state.groupName)
                }) {
                    WeSettingValue(state.groupName.ifBlank { "未命名" })
                }
                WeSettingItem("群二维码", onClick = { showQrCode = true }) {
                    Icon(
                        painterResource(R.drawable.ic_qrcode_outlined),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = WeTheme.colorScheme.textSecondary
                    )
                }
                WeSettingItem(
                    "群公告",
                    description = state.announcement.ifBlank { null },
                    onClick = { openEditor(GroupEditor.Announcement, state.announcement) }
                )
                WeSettingItem(
                    "群管理",
                    description = if (state.canManageMembers) "成员管理" else "仅群主和管理员可操作",
                    onClick = if (state.canManageMembers) ({ showRemoveMembers = true }) else null
                )
                WeSettingItem("备注", showDivider = false, onClick = {
                    openEditor(GroupEditor.Remark, state.remark)
                }) {
                    WeSettingValue(state.remark)
                }
            }

            WeSettingItem("查找聊天记录", showDivider = false)

            WeSettingGroup {
                WeSettingItem("消息免打扰", showArrow = false) {
                    WeSwitch(state.isMuted, onChange = viewModel::setMuted)
                }
                WeSettingItem("折叠该聊天", showArrow = false) {
                    WeSwitch(state.isFolded, onChange = viewModel::setFolded)
                }
                WeSettingItem(
                    label = "以下消息仍通知",
                    description = "@我、@所有人和群公告"
                )
                WeSettingItem("置顶聊天", showArrow = false) {
                    WeSwitch(state.isPinned, onChange = viewModel::setPinned)
                }
                WeSettingItem("保存到通讯录", showArrow = false, showDivider = false) {
                    WeSwitch(state.saveToContacts, onChange = viewModel::setSaveToContacts)
                }
            }

            WeSettingGroup {
                WeSettingItem("我在群里的昵称", onClick = {
                    openEditor(GroupEditor.Nickname, state.myNickname)
                }) {
                    WeSettingValue(state.myNickname)
                }
                WeSettingItem("显示群成员昵称", showArrow = false, showDivider = false) {
                    WeSwitch(
                        state.showMemberNicknames,
                        onChange = viewModel::setShowMemberNicknames
                    )
                }
            }

            ChatBackgroundSetting(
                label = "设置当前聊天背景",
                value = state.backgroundPath,
                onChange = viewModel::updateBackground
            )
            WeSettingGroup {
                WeSettingItem(
                    "清空聊天记录",
                    onClick = { confirmAction = ConfirmAction.Clear }
                )
                WeSettingItem("投诉", showDivider = false)
            }
            WeDangerButton("退出群聊") {
                confirmAction = ConfirmAction.Exit
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    editor?.let { type ->
        AlertDialog(
            onDismissRequest = { editor = null },
            title = { Text(type.title) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = type != GroupEditor.Announcement
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (type) {
                        GroupEditor.Name -> viewModel.updateName(editText)
                        GroupEditor.Announcement -> viewModel.updateAnnouncement(editText)
                        GroupEditor.Remark -> viewModel.updateRemark(editText)
                        GroupEditor.Nickname -> viewModel.updateMyNickname(editText)
                    }
                    editor = null
                }) { Text("完成") }
            },
            dismissButton = {
                TextButton(onClick = { editor = null }) { Text("取消") }
            }
        )
    }
    if (showRemoveMembers) {
        AlertDialog(
            onDismissRequest = { showRemoveMembers = false },
            title = { Text("群管理") },
            text = {
                Column {
                    TextButton(onClick = {
                        showRemoveMembers = false
                        pickMembers(200)
                    }) { Text("添加群成员") }
                    state.members.forEach { member ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.removeMember(member.id)
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(member.name)
                            Text("移除", color = WeTheme.colorScheme.danger)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRemoveMembers = false }) { Text("完成") }
            }
        )
    }
    if (showQrCode) {
        AlertDialog(
            onDismissRequest = { showQrCode = false },
            title = { Text("群二维码") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WeQRCode(qrCodeState, Modifier.size(220.dp))
                    Text(
                        "扫一扫上面的二维码，加入该群聊",
                        color = WeTheme.colorScheme.textSecondary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrCode = false }) { Text("完成") }
            }
        )
    }
    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(if (action == ConfirmAction.Clear) "清空聊天记录" else "退出群聊") },
            text = {
                Text(
                    if (action == ConfirmAction.Clear) "清空后将无法恢复聊天记录。"
                    else "退出后将不再接收此群聊消息。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (action == ConfirmAction.Clear) {
                        viewModel.clearMessages()
                    } else {
                        viewModel.exitGroup()
                        onExitGroup()
                    }
                    confirmAction = null
                }) { Text("确定", color = WeTheme.colorScheme.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("取消") }
            }
        )
    }
}

private enum class GroupEditor(val title: String) {
    Name("群聊名称"),
    Announcement("群公告"),
    Remark("群聊备注"),
    Nickname("我在群里的昵称")
}

private enum class ConfirmAction { Clear, Exit }
