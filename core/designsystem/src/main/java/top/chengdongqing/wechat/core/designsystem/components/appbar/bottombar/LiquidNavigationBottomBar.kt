package top.chengdongqing.wechat.core.designsystem.components.appbar.bottombar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun <T : NavigationTab> WeLiquidNavigationBottomBar(
    tabs: List<T>,
    badgeMap: Map<T, Int>,
    currentTabIndex: Int,
    modifier: Modifier = Modifier,
    selectedTabPosition: Float = currentTabIndex.toFloat(),
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState? = null,
) {
    val shape = RoundedCornerShape(50.dp)
    val glassModifier = if (hazeState != null) {
        Modifier.hazeEffect(hazeState, style = HazeMaterials.ultraThin(WeTheme.colorScheme.surface))
    } else {
        Modifier.background(Color.White.copy(alpha = 0.72f))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.16f)
            )
            .then(glassModifier)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.72f), shape)
            .height(58.dp),
    ) {
        val itemWidth = maxWidth / tabs.size
        LiquidSelectionPill(itemWidth, selectedTabPosition)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                LiquidTabItem(
                    tab = tab,
                    badge = badgeMap[tab] ?: 0,
                    onClick = { onTabSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun LiquidSelectionPill(itemWidth: Dp, position: Float) {
    val offsetX by animateDpAsState(
        targetValue = itemWidth * position,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 540f),
        label = "liquid-nav-pill-offset",
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(offsetX.roundToPx(), 0)
            }
            .width(itemWidth)
            .fillMaxHeight()
            .padding(6.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color.Black.copy(alpha = 0.075f)),
    )
}

@Composable
private fun RowScope.LiquidTabItem(
    tab: NavigationTab,
    badge: Int,
    onClick: () -> Unit,
) {
    val contentColor = WeTheme.colorScheme.textPrimary

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .onTap(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WeBadge(
            visible = badge > 0,
            content = badge.toBadgeText(),
            size = 16.dp,
        ) {
            Icon(
                painter = painterResource(tab.selectedIcon),
                contentDescription = stringResource(tab.label),
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp),
            )
        }
        Text(
            text = stringResource(tab.label),
            color = contentColor,
            fontSize = 11.sp,
        )
    }
}
