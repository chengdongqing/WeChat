package top.chengdongqing.wechat.ui.contacts.detail

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.ui.components.switch.WeSwitch
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.theme.White

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
        containerColor = Color(0xFFEDEDED)
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
                            color = WeChatTheme.colorScheme.textSecondary
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(White)
                    .clickable {},
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "删除",
                    color = WeChatTheme.colorScheme.error,
                    fontSize = 17.sp
                )
            }
        }
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
        MenuListItem(label, content = content, onClick = onClick)

        if (showDivider) {
            WeDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}