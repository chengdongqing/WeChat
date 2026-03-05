package top.chengdongqing.wechat.features.settings.ui.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun AddMeMethodSettingScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            WeTopBar(title = "添加我的方式", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WeSettingGroup("可通过以下方式添加我为好友") {
                WeSettingItem(
                    label = "二维码",
                    showArrow = false
                ) {
                    WeSwitch(checked = true)
                }
                WeSettingItem(
                    label = "碰一碰",
                    showArrow = false
                ) {
                    WeSwitch(checked = true)
                }
                WeSettingItem(
                    label = "雷达",
                    showArrow = false
                ) {
                    WeSwitch(checked = true)
                }
                WeSettingItem(
                    label = "名片",
                    showArrow = false
                ) {
                    WeSwitch(checked = true)
                }
                WeSettingItem(
                    label = "其他",
                    description = "删除的联系人保留的聊天等",
                    showArrow = false,
                    showDivider = false,
                    height = 68.dp
                ) {
                    WeSwitch(checked = true)
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}