package top.chengdongqing.wechat.features.contacts.ui.detail.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.util.toYearMonthDisplay
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactDetailViewModel
import top.chengdongqing.wechat.features.me.domain.model.Gender.Companion.safePronoun

@Composable
fun ContactProfileScreen(
    contactId: String,
    onBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val contact = viewModel.contact.collectAsState().value ?: return

    Scaffold(
        topBar = {
            WeTopBar(title = "朋友资料", onBack = onBack)
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
            WeSettingGroup("备注") {
                WeSettingItem(
                    label = "备注名",
                    trailing = {
                        WeSettingValue(
                            text = contact.remarkName,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    },
                    onClick = onNavigateToEdit
                )
                WeSettingItem(
                    label = "电话",
                    onClick = onNavigateToEdit
                )
                WeSettingItem(
                    label = "标签",
                    onClick = onNavigateToEdit
                )
                WeSettingItem(
                    label = "备忘",
                    trailing = {
                        WeSettingValue(
                            text = contact.note,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    },
                    onClick = onNavigateToEdit
                )
                WeSettingItem(
                    label = "照片",
                    showDivider = false,
                    onClick = onNavigateToEdit
                )
            }

            if (contact.isFriend) {
                WeSettingGroup("朋友权限") {
                    WeSettingItem(
                        label = "权限",
                        trailing = { WeSettingValue("聊天、朋友圈、微信运动等") },
                        showDivider = false
                    )
                }
                WeSettingGroup("更多信息") {
                    WeSettingItem(
                        label = "我和${stringResource(contact.gender.safePronoun)}的共同群聊",
                        trailing = { WeSettingValue("0个") }
                    )
                    contact.signature?.let {
                        WeSettingItem(
                            label = "签名",
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
                            label = "来源",
                            trailing = { WeSettingValue(it.getDescription(contact.isFromMe)) },
                            showArrow = false
                        )
                    }
                    contact.addedAt?.let {
                        WeSettingItem(
                            label = "添加时间",
                            trailing = { WeSettingValue(it.toYearMonthDisplay()) },
                            showArrow = false,
                            showDivider = false
                        )
                    }
                }
            }
        }
    }
}