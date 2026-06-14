package top.chengdongqing.wechat.feature.contacts.ui.detail.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.common.util.toYearMonthDisplay
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.ui.getDescription
import top.chengdongqing.wechat.core.designsystem.ui.safePronounRes
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactDetailViewModel

@Composable
fun ContactProfileScreen(
    onBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: ContactDetailViewModel
) {
    val contact by viewModel.contact.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.contact_profile_title),
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
            contact?.let {
                ContactProfileContent(
                    contact = it,
                    onNavigateToEdit = onNavigateToEdit
                )
            }
        }
    }
}

@Composable
private fun ContactProfileContent(
    contact: Contact,
    onNavigateToEdit: () -> Unit
) {
    val resources = LocalResources.current

    WeSettingGroup(stringResource(R.string.contact_profile_section_remark)) {
        WeSettingItem(
            label = stringResource(R.string.contact_profile_remark_name),
            trailing = {
                WeSettingValue(
                    text = contact.remarkName,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            },
            onClick = onNavigateToEdit
        )
        WeSettingItem(
            label = stringResource(R.string.contact_profile_remark_phone),
            onClick = onNavigateToEdit
        )
        WeSettingItem(
            label = stringResource(R.string.contact_profile_remark_tags),
            onClick = onNavigateToEdit
        )
        WeSettingItem(
            label = stringResource(R.string.contact_profile_remark_note),
            trailing = {
                WeSettingValue(
                    text = contact.note,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            },
            onClick = onNavigateToEdit
        )
        WeSettingItem(
            label = stringResource(R.string.contact_profile_remark_photos),
            showDivider = false,
            onClick = onNavigateToEdit
        )
    }

    if (contact.isFriend) {
        WeSettingGroup(stringResource(R.string.contact_profile_section_permissions)) {
            WeSettingItem(
                label = stringResource(R.string.contact_profile_permissions_label),
                trailing = {
                    WeSettingValue(
                        text = stringResource(R.string.contact_profile_permissions_value),
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                },
                showDivider = false
            )
        }
        WeSettingGroup(stringResource(R.string.contact_profile_section_more)) {
            WeSettingItem(
                label = stringResource(
                    R.string.contact_profile_common_groups,
                    stringResource(contact.gender.safePronounRes)
                ),
                trailing = {
                    WeSettingValue(
                        stringResource(
                            R.string.contact_profile_common_groups_count,
                            0
                        )
                    )
                }
            )
            contact.signature?.let {
                WeSettingItem(
                    label = stringResource(R.string.contact_profile_signature),
                    trailing = {
                        WeSettingValue(
                            text = it,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    },
                    showArrow = false
                )
            }
            contact.source?.let {
                WeSettingItem(
                    label = stringResource(R.string.contact_profile_source),
                    trailing = {
                        WeSettingValue(
                            it.getDescription(
                                resources,
                                contact.isFromMe
                            )
                        )
                    },
                    showArrow = false
                )
            }
            contact.addedAt?.let {
                WeSettingItem(
                    label = stringResource(R.string.contact_profile_added_at),
                    trailing = {
                        val result =
                            it.toYearMonthDisplay(LocalAppearanceSetting.current.appLanguage)
                        WeSettingValue(result)
                    },
                    showArrow = false,
                    showDivider = false
                )
            }
        }
    }
}