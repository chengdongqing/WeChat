package top.chengdongqing.wechat.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.ui.theme.Danger
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun DiscoveryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeChatTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuListItem("朋友圈", R.drawable.ic_moments_outline)
        MenuListItem("搜一搜", R.drawable.ic_search_logo_outline, Danger)
    }
}