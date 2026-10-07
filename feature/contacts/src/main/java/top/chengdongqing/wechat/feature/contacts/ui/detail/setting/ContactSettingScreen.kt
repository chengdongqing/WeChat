package top.chengdongqing.wechat.feature.contacts.ui.detail.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.dialog.DialogManager
import top.chengdongqing.wechat.core.designsystem.components.menu.WeDangerButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.theme.Red100
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.ContactRelation
import top.chengdongqing.wechat.core.navigation.LocalAppNavigator
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.contacts.R
import top.chengdongqing.wechat.feature.contacts.ui.picker.ContactPickerRequest
import top.chengdongqing.wechat.feature.contacts.ui.picker.rememberContactPickerLauncher
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.contacts.R as ContactsR

@Composable
fun ContactSettingRoute(
    contactId: String,
    viewModel: ContactSettingViewModel = hiltViewModel { factory: ContactSettingViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigator = LocalAppNavigator.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is ContactSettingUiEvent.OnContactDeleted -> {
                        navigator.clear()
                        navigator.navigateTo(ScreenRoute.Home)
                    }
                }
            }
        }
    }

    ContactSettingScreen(
        state = state,
        onIntent = { intent ->
            when (intent) {
                is ContactSettingUiIntent.Back -> navigator.back()
                is ContactSettingUiIntent.NavigateTo -> navigator.navigateTo(intent.route)
                else -> viewModel.onIntent(intent)
            }
        }
    )
}

@Composable
fun ContactSettingScreen(
    state: ContactSettingUiState = ContactSettingUiState(),
    onIntent: (ContactSettingUiIntent) -> Unit = {},
) {
    val contact = state.contact

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.contact_settings_title),
                onBack = { onIntent(ContactSettingUiIntent.Back) }
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
            contact?.let {
                ContactSettingContent(
                    contact = it,
                    onIntent = onIntent,
                )
            }
        }
    }
}

@Composable
private fun ContactSettingContent(
    contact: Contact,
    onIntent: (ContactSettingUiIntent) -> Unit,
) {
    val resources = LocalResources.current
    val contactPicker = rememberContactPickerLauncher { contacts ->
        DialogManager.show(
            title = resources.getString(ContactsR.string.msg_confirm_send),
            okText = DesignR.string.action_send
        ) {
            onIntent(ContactSettingUiIntent.ShareContact(contacts.first().id))
        }
    }

    WeSettingGroup {
        WeSettingItem(
            label = stringResource(R.string.contact_settings_profile),
            onClick = {
                onIntent(ContactSettingUiIntent.NavigateTo(ScreenRoute.EditContactProfile(contact.id)))
            }
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
                    contactPicker.launch(ContactPickerRequest(maxSelection = 1))
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
            WeSwitch(checked = contact.isStarred) {
                onIntent(ContactSettingUiIntent.ToggleStar)
            }
        }
    }
    WeSettingGroup {
        WeSettingItem(
            label = stringResource(R.string.contact_settings_block),
            showArrow = false
        ) {
            WeSwitch(checked = contact.isBlocked) {
                onIntent(ContactSettingUiIntent.ToggleBlock)
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
            onIntent(ContactSettingUiIntent.DeleteContact)
        }
    }
}

@Composable
private fun DeleteButton(contact: Contact, onDelete: () -> Unit) {
    val resources = LocalResources.current

    val showDialog = {
        DialogManager.show(
            title = resources.getString(ContactsR.string.contact_delete_title, contact.displayName),
            content = resources.getString(ContactsR.string.contact_delete_content),
            okColor = Red100,
            okText = DesignR.string.action_delete,
            onOk = onDelete
        )
    }

    WeDangerButton(
        label = stringResource(DesignR.string.action_delete),
        onClick = showDialog
    )
}

@Preview
@Composable
private fun ContactSettingPreview() {
    WeTheme {
        ContactSettingScreen(
            state = ContactSettingUiState(
                contact = Contact(
                    id = "wxid_1212",
                    nickname = "海盐芝士不加糖",
                    relation = ContactRelation.Friend,
                )
            )
        )
    }
}
