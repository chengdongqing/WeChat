package top.chengdongqing.wechat.features.home.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
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
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBarIcon
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.contacts.navigation.ContactsRoute
import top.chengdongqing.wechat.features.home.model.HomeTab
import top.chengdongqing.wechat.features.home.model.QuickAction
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel

@Composable
fun HomeTopBarWrapper(
    currentTab: HomeTab,
    viewModel: ProfileViewModel,
    navController: NavHostController,
    unreadCounts: Map<HomeTab, Int>
) {
    if (currentTab.route != HomeTab.Me.route) {
        val title = currentTab.getDisplayTitle(unreadCounts)

        HomeTopBar(
            title = title,
            viewModel = viewModel,
            onNavigateToAddFriend = {
                navController.navigate(ContactsRoute.AddContact.route)
            }
        )
    } else {
        Surface(
            color = WeTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(50.dp)
            ) {}
        }
    }
}

/**
 * 首页顶部栏
 */
@Composable
private fun HomeTopBar(
    title: String,
    viewModel: ProfileViewModel,
    onNavigateToAddFriend: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var anchorPosition by remember { mutableStateOf(Offset.Zero) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }

    val launchScanner = rememberScanCodeLauncher { qrCodes ->
        viewModel.handleScannedQRCode(qrCodes.first())
    }

    Column {
        WeTopBar(title = title) {
            ActionIcon(
                icon = R.drawable.ic_search_outlined,
                description = "搜索"
            )

            WeTopBarIcon(
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
        onDismiss = { menuExpanded = false }
    ) { action ->
        when (action) {
            QuickAction.AddFriend -> onNavigateToAddFriend()
            QuickAction.Scan -> launchScanner()
            else -> {}
        }
    }
}

@Composable
private fun HomeTab.getDisplayTitle(unreadCounts: Map<HomeTab, Int>): String {
    val tabLabel = stringResource(label)

    return when {
        this == HomeTab.Chats -> {
            val unread = unreadCounts[this] ?: 0
            if (unread > 0) "$tabLabel($unread)" else tabLabel
        }

        else -> tabLabel
    }
}