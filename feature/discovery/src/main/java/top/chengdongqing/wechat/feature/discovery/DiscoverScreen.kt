package top.chengdongqing.wechat.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeMenuListItem
import top.chengdongqing.wechat.core.designsystem.theme.Red100
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.LocalAppNavigator
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.discovery.R as FeatureR

@Composable
fun DiscoverRoute() {
    val navigator = LocalAppNavigator.current

    DiscoverScreen(
        onNavigate = navigator::navigateTo
    )
}

@Composable
fun DiscoverScreen(
    onNavigate: (ScreenRoute) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeMenuListItem(
            label = stringResource(FeatureR.string.discover_menu_moments),
            icon = R.drawable.ic_moments_outlined_colorful,
            onClick = {
                onNavigate(ScreenRoute.Moments)
            }
        )
        WeMenuListItem(
            label = stringResource(FeatureR.string.discover_menu_search),
            icon = R.drawable.ic_search_logo_outlined,
            iconColor = Red100,
            onClick = {}
        )
        WeMenuListItem(
            label = stringResource(FeatureR.string.discover_menu_intercom),
            icon = R.drawable.ic_mic2_filled,
            iconColor = WeTheme.colorScheme.primary,
            onClick = {
                onNavigate(ScreenRoute.IntercomLobby)
            }
        )
    }
}

@Preview
@Composable
private fun DiscoverPreview() {
    WeTheme {
        DiscoverScreen()
    }
}