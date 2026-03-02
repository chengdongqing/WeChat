package top.chengdongqing.wechat.features.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun DiscoveryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuListItem("朋友圈", R.drawable.ic_moments_outlined_colorful)
        MenuListItem("搜一搜", R.drawable.ic_search_logo_outlined, Danger)
    }
}