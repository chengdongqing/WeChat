package top.chengdongqing.wechat.app.shell

import androidx.compose.animation.core.MutableTransitionState
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
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.qrcode.scanner.rememberQrCodeScannerLauncher

@Composable
fun MainTopBar(
    currentTab: MainTab,
    unreadMap: Map<MainTab, Int>,
    onGroupChat: () -> Unit,
    onAddFriend: () -> Unit,
    onPayment: () -> Unit,
    onScannedQrCode: (String) -> Unit
) {
    when {
        currentTab != MainTab.Me -> {
            val title = currentTab.getDisplayTitle(unreadMap)

            TopBarContent(
                title = title,
                onGroupChat = onGroupChat,
                onAddFriend = onAddFriend,
                onPayment = onPayment,
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
    onGroupChat: () -> Unit,
    onAddFriend: () -> Unit,
    onPayment: () -> Unit,
    onScannedQrCode: (String) -> Unit
) {
    val menuExpanded = remember { MutableTransitionState(false) }
    var anchorPosition by remember { mutableStateOf(Offset.Zero) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }

    val qrCodeScanner = rememberQrCodeScannerLauncher { qrCodes ->
        qrCodes.firstOrNull()?.let(onScannedQrCode)
    }

    val handleDismiss = {
        menuExpanded.targetState = false
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
                menuExpanded.targetState = true
            }
        }

        WeDivider()
    }

    QuickActionsMenu(
        visibleState = menuExpanded,
        anchorPosition = anchorPosition,
        anchorSize = anchorSize,
        onDismiss = handleDismiss
    ) { action ->
        when (action) {
            QuickAction.GroupChat -> onGroupChat()
            QuickAction.AddFriend -> onAddFriend()
            QuickAction.Scan -> qrCodeScanner.launch()
            QuickAction.Payment -> onPayment()
        }
    }
}

@Composable
private fun MainTab.getDisplayTitle(unreadMap: Map<MainTab, Int>): String {
    val tabLabel = stringResource(label)

    return when {
        this == MainTab.Chats -> {
            val unread = unreadMap[this] ?: 0
            if (unread > 0) "$tabLabel($unread)" else tabLabel
        }

        else -> tabLabel
    }
}
