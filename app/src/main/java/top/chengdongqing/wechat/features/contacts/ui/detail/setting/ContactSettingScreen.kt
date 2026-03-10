package top.chengdongqing.wechat.features.contacts.ui.detail.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.menu.WeDangerButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactDetailViewModel
import top.chengdongqing.wechat.features.contacts.ui.detail.NavigationEvent

@Composable
fun ContactSettingScreen(
    contactId: String,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToContactProfile: () -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val contact = viewModel.contact.collectAsState().value ?: return

    // 处理导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.ContactDeleted -> onDelete()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            WeTopBar(title = "朋友设置", onBack = onBack)
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
            WeSettingGroup {
                WeSettingItem("设置朋友资料", onClick = onNavigateToContactProfile) {
                    WeSettingValue(contact.displayName)
                }
                WeSettingItem("朋友权限", showDivider = false)
            }
            if (contact.isFriend) {
                WeSettingGroup {
                    WeSettingItem("把他（她）推荐给朋友")
                    WeSettingItem("添加到桌面", showDivider = false)
                }
                WeSettingItem(
                    label = "设为星标朋友",
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch()
                }
            }
            WeSettingGroup {
                WeSettingItem(label = "加入黑名单", showArrow = false) {
                    WeSwitch(checked = contact.isBlocked) {
                        viewModel.toggleBlock()
                    }
                }
                WeSettingItem("投诉", showDivider = false)
            }

            if (contact.isFriend) {
                DeleteButton(contact) {
                    viewModel.deleteContact()
                }
            }
        }
    }
}

@Composable
private fun DeleteButton(contact: Contact, onDelete: () -> Unit) {
    val dialog = rememberDialogState()
    val resources = LocalResources.current

    val showDialog = {
        dialog.show(
            title = resources.getString(R.string.contact_delete_title, contact.displayName),
            content = resources.getString(R.string.contact_delete_content),
            okColor = Danger,
            okText = R.string.action_delete,
            onOk = onDelete
        )
    }

    WeDangerButton(
        label = stringResource(R.string.action_delete),
        onClick = showDialog
    )
}