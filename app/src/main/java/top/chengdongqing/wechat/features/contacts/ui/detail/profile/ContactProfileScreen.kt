package top.chengdongqing.wechat.features.contacts.ui.detail.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.util.toChatDisplayTime
import top.chengdongqing.wechat.data.model.Gender.Companion.safePronoun
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactDetailViewModel

@Composable
fun ContactProfileScreen(
    contactId: String,
    onBack: () -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel { factory: ContactDetailViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val contact = uiState.contact ?: return

    Scaffold(
        topBar = {
            WeTopBar("朋友资料", onBack = onBack)
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
            ProfileGroup("备注") {
                ProfileItem(
                    label = "备注名",
                    trailing = { ProfileItemText(contact.displayName) }
                )
                ProfileItem(
                    label = "电话"
                )
                ProfileItem(
                    label = "标签"
                )
                ProfileItem(
                    label = "备忘",
                    trailing = { ProfileItemText(contact.note) }
                )
                ProfileItem(
                    label = "备注名",
                    showDivider = false
                )
            }
            ProfileGroup("朋友权限") {
                ProfileItem(
                    label = "权限",
                    trailing = { ProfileItemText("聊天、朋友圈、微信运动等") },
                    showDivider = false
                )
            }
            ProfileGroup("更多信息") {
                ProfileItem(
                    label = "我和${contact.gender.safePronoun}的共同群聊",
                    trailing = { ProfileItemText("0个") }
                )
                ProfileItem(
                    label = "签名",
                    trailing = { ProfileItemText(contact.signature) },
                    showArrow = false
                )
                contact.source?.let {
                    ProfileItem(
                        label = "来源",
                        trailing = { ProfileItemText(it.getDescription(contact.isFromMe)) },
                        showArrow = false
                    )
                }
                ProfileItem(
                    label = "添加时间",
                    trailing = { ProfileItemText(contact.addedAt?.toChatDisplayTime()) },
                    showArrow = false,
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun ProfileGroup(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        color = WeTheme.colorScheme.textSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    Column(modifier = Modifier.background(White)) {
        content()
    }
}

@Composable
private fun ProfileItem(
    label: String,
    showDivider: Boolean = true,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    MenuListItem(
        label = label,
        trailing = trailing,
        height = 52.dp,
        showArrow = showArrow,
        onClick = onClick
    )

    if (showDivider) {
        WeDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun ProfileItemText(text: String?) {
    text?.let {
        Text(
            text = text,
            fontSize = 16.sp,
            color = WeTheme.colorScheme.textSecondary
        )
    }
}