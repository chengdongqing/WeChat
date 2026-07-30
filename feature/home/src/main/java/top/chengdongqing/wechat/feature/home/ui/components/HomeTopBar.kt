package top.chengdongqing.wechat.feature.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import top.chengdongqing.wechat.core.common.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.home.model.HomeTab
import top.chengdongqing.wechat.feature.home.model.QuickAction

@Composable
fun HomeTopBar(
    currentTab: HomeTab,
    unreadMap: Map<HomeTab, Int>,
    onNavigateToGroupChat: () -> Unit,
    onNavigateToAddFriend: () -> Unit,
    onNavigateToPayment: () -> Unit,
    onScannedQrCode: (String) -> Unit
) {
    when {
        currentTab != HomeTab.Me -> {
            val title = currentTab.getDisplayTitle(unreadMap)

            TopBarContent(
                title = title,
                onNavigateToGroupChat = onNavigateToGroupChat,
                onNavigateToAddFriend = onNavigateToAddFriend,
                onNavigateToPayment = onNavigateToPayment,
                onScannedQrCode = onScannedQrCode
            )
        }

        else -> {
            Box(
                modifier = Modifier
                    .background(WeTheme.colorScheme.surface)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(50.dp)
            )
        }
    }
}

/**
 * 首页顶部栏
 */
@Composable
private fun TopBarContent(
    title: String,
    onNavigateToGroupChat: () -> Unit,
    onNavigateToAddFriend: () -> Unit,
    onNavigateToPayment: () -> Unit,
    onScannedQrCode: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var anchorPosition by remember { mutableStateOf(Offset.Zero) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }

    val launchScanner = rememberScanCodeLauncher { qrCodes ->
        qrCodes.firstOrNull()?.let(onScannedQrCode)
    }

    val handleDismiss = {
        menuExpanded = false
    }

    Column {
        WeTopAppBar(title = title) {
            IconButton(
                icon = R.drawable.ic_search_outlined,
                description = stringResource(R.string.action_search)
            )

            IconButton(
                modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                    anchorPosition = layoutCoordinates.positionInWindow()
                    anchorSize = layoutCoordinates.size
                },
                icon = R.drawable.ic_plus_circle_outlined,
                description = stringResource(R.string.action_more)
            ) {
                menuExpanded = true
            }
        }

        WeDivider()
    }

    QuickActions(
        expanded = menuExpanded,
        anchorPosition = anchorPosition,
        anchorSize = anchorSize,
        onDismiss = handleDismiss
    ) { action ->
        when (action) {
            QuickAction.GroupChat -> onNavigateToGroupChat()
            QuickAction.AddFriend -> onNavigateToAddFriend()
            QuickAction.Scan -> launchScanner()
            QuickAction.Payment -> onNavigateToPayment()
        }
    }
}

@Composable
private fun HomeTab.getDisplayTitle(unreadMap: Map<HomeTab, Int>): String {
    val tabLabel = stringResource(labelRes)

    return when {
        this == HomeTab.Chats -> {
            val unread = unreadMap[this] ?: 0
            if (unread > 0) "$tabLabel($unread)" else tabLabel
        }

        else -> tabLabel
    }
}
