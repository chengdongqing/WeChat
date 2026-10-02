package top.chengdongqing.wechat.home

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
internal fun HomeTopBar(
    currentTab: HomeTab,
    unreadMap: Map<HomeTab, Int>,
    actions: HomeActions,
) {
    when (currentTab) {
        HomeTab.Me -> {
            // 空白占位
            Placeholder()
        }

        else -> {
            StandardHomeTopBar(
                title = currentTab.getTitle(unreadMap),
                actions = actions
            )
        }
    }
}

@Composable
private fun StandardHomeTopBar(
    title: String,
    actions: HomeActions
) {
    val menuExpanded = remember { MutableTransitionState(false) }
    var anchorPosition by remember { mutableStateOf(Offset.Zero) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }

    val handleDismiss = {
        menuExpanded.targetState = false
    }

    WeTopAppBar(
        title = title,
        showDivider = true
    ) {
        IconButton(
            icon = R.drawable.ic_search_outlined,
            description = stringResource(R.string.action_search),
            onClick = actions.onSearchClick
        )

        IconButton(
            modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                anchorPosition = layoutCoordinates.positionInWindow()
                anchorSize = layoutCoordinates.size
            },
            icon = R.drawable.ic_plus_circle_outlined,
            description = stringResource(R.string.action_more)
        ) {
            menuExpanded.targetState = true
        }
    }

    QuickActionsMenu(
        visibleState = menuExpanded,
        anchorPosition = anchorPosition,
        anchorSize = anchorSize,
        onDismiss = handleDismiss,
        onAction = actions.onQuickActionClick
    )
}

@Composable
private fun Placeholder() {
    Box(
        Modifier
            .background(WeTheme.colorScheme.surface)
            .fillMaxWidth()
            .statusBarsPadding()
            .height(50.dp)
    )
}

@Composable
private fun HomeTab.getTitle(unreadMap: Map<HomeTab, Int>): String {
    val tabLabel = stringResource(label)

    return when (this) {
        HomeTab.Chats -> {
            val unread = unreadMap[this] ?: 0
            if (unread > 0) "$tabLabel($unread)" else tabLabel
        }

        else -> tabLabel
    }
}
