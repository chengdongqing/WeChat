package top.chengdongqing.wechat.features.contacts.ui.detail.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.features.contacts.model.Contact
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactAction
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactDetailViewModel

@Composable
fun ContactSettingScreen(
    contactId: String,
    onBack: () -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val contact = uiState.contact

    Scaffold(
        topBar = {
            WeTopBar("朋友设置", onBack = onBack)
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
            Column {
                SettingItem(
                    label = "设置朋友资料",
                    content = {
                        Text(
                            text = contact.remarkName,
                            fontSize = 16.sp,
                            color = WeTheme.colorScheme.textSecondary
                        )
                    }
                )
                SettingItem("朋友权限", showDivider = false)
            }
            Column {
                SettingItem("把他（她）推荐给朋友")
                SettingItem("添加到桌面", showDivider = false)
            }
            SettingItem("设为星标朋友", showDivider = false) {
                WeSwitch()
            }
            Column {
                SettingItem("加入黑名单") {
                    WeSwitch()
                }
                SettingItem("投诉", showDivider = false)
            }

            DeleteButton(contact) {
                viewModel.handleAction(ContactAction.DeleteContact)
            }
        }
    }
}

@Composable
private fun DeleteButton(contact: Contact, onDelete: () -> Unit) {
    val dialog = rememberDialogState()

    val showDialog = {
        dialog.show(
            title = "即将删除联系人“${contact.remarkName}”",
            content = "删除后对方不会收到通知",
            okColor = Danger,
            okText = "删除",
            onOk = onDelete
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(White)
            .clickable { showDialog() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "删除",
            color = WeTheme.colorScheme.error,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingItem(
    label: String,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.background(White)) {
        MenuListItem(label, content = content, height = 52.dp, onClick = onClick)

        if (showDivider) {
            WeDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}