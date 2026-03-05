package top.chengdongqing.wechat.features.contacts.ui.detail.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingValue
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
            SettingGroup("备注") {
                SettingItem(
                    label = "备注名",
                    trailing = { SettingValue(contact.remarkName) },
                    onClick = onNavigateToEdit
                )
                SettingItem(
                    label = "电话",
                    onClick = onNavigateToEdit
                )
                SettingItem(
                    label = "标签",
                    onClick = onNavigateToEdit
                )
                SettingItem(
                    label = "备忘",
                    trailing = { SettingValue(contact.note) },
                    onClick = onNavigateToEdit
                )
                SettingItem(
                    label = "照片",
                    showDivider = false,
                    onClick = onNavigateToEdit
                )
            }
            SettingGroup("朋友权限") {
                SettingItem(
                    label = "权限",
                    trailing = { SettingValue("聊天、朋友圈、微信运动等") },
                    showDivider = false
                )
            }
            SettingGroup("更多信息") {
                SettingItem(
                    label = "我和${contact.gender.safePronoun}的共同群聊",
                    trailing = { SettingValue("0个") }
                )
                contact.signature?.let {
                    SettingItem(
                        label = "签名",
                        trailing = { SettingValue(it) },
                        showArrow = false
                    )
                }
                contact.source?.let {
                    SettingItem(
                        label = "来源",
                        trailing = { SettingValue(it.getDescription(contact.isFromMe)) },
                        showArrow = false
                    )
                }
                SettingItem(
                    label = "添加时间",
                    trailing = { SettingValue(contact.addedAt?.toYearMonthDisplay()) },
                    showArrow = false,
                    showDivider = false
                )
            }
        }
    }
}