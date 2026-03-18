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
import top.chengdongqing.wechat.features.contacts.ui.picker.rememberPickContactLauncher

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

    val dialog = rememberDialogState()
    val resources = LocalResources.current
    val pickContact = rememberPickContactLauncher { contacts ->
        dialog.show(
            title = resources.getString(R.string.msg_confirm_send),
            okText = R.string.action_send
        ) {
            viewModel.sendContactCard(contacts.first().id)
        }
    }

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.contact_settings_title),
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
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.contact_settings_profile),
                    onClick = onNavigateToContactProfile
                ) {
                    WeSettingValue(contact.displayName)
                }
                WeSettingItem(
                    label = stringResource(R.string.contact_settings_permissions),
                    showDivider = false,
                    onClick = {}
                )
            }
            if (contact.isFriend) {
                WeSettingGroup {
                    WeSettingItem(
                        label = stringResource(R.string.contact_settings_recommend),
                        onClick = {
                            pickContact(1)
                        }
                    )
                    WeSettingItem(
                        label = stringResource(R.string.contact_settings_add_to_desktop),
                        showDivider = false,
                        onClick = {}
                    )
                }
                WeSettingItem(
                    label = stringResource(R.string.contact_settings_star),
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch()
                }
            }
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.contact_settings_block),
                    showArrow = false
                ) {
                    WeSwitch(checked = contact.isBlocked) {
                        viewModel.toggleBlock()
                    }
                }
                WeSettingItem(
                    label = stringResource(R.string.contact_settings_report),
                    showDivider = false,
                    onClick = {}
                )
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