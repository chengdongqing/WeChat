package top.chengdongqing.wechat.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeMenuListItem
import top.chengdongqing.wechat.core.designsystem.theme.SemanticError
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun DiscoveryScreen(onNavigateToMoments: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeMenuListItem(
            label = stringResource(R.string.discover_menu_moments),
            icon = R.drawable.ic_moments_outlined_colorful,
            onClick = onNavigateToMoments
        )
        WeMenuListItem(
            label = stringResource(R.string.discover_menu_search),
            icon = R.drawable.ic_search_logo_outlined,
            iconColor = SemanticError,
            onClick = {}
        )
    }
}
