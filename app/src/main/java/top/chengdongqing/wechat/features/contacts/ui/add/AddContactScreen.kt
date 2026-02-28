package top.chengdongqing.wechat.features.contacts.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.QrDotStyle
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.WeQRCode
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.rememberQRCodeState
import top.chengdongqing.wechat.core.designsystem.components.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.RequestAddFriendPermission
import top.chengdongqing.wechat.core.designsystem.util.rememberScreenFractionWidth
import top.chengdongqing.wechat.features.me.ui.profile.HandleProfileNavigationEvents
import top.chengdongqing.wechat.features.me.ui.profile.ProfileUiState
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel

@Composable
fun AddContactScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToNFC: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToGroup: () -> Unit,
    onNavigateToContactDetail: (contactId: String) -> Unit,
    onNavigateToPlainText: (text: String) -> Unit,
    onNavigateToWebView: (url: String) -> Unit,
) {
    RequestAddFriendPermission(onRevoked = onBack) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        // 生成二维码
        LaunchedEffect(Unit) {
            viewModel.generateQRCode()
        }
        // 处理扫码
        val launchScanner = rememberScanCodeLauncher { qrCodes ->
            viewModel.handleScannedQRCode(qrCodes.first())
        }

        // 处理导航事件
        HandleProfileNavigationEvents(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            onNavigateToContactDetail = onNavigateToContactDetail,
            onNavigateToPlainText = onNavigateToPlainText,
            onNavigateToWebView = onNavigateToWebView
        )

        val addFriendOptions = rememberAddFriendOptions(
            launchScanner = launchScanner,
            onNavigateToNFC = onNavigateToNFC,
            onNavigateToRadar = onNavigateToRadar,
            onNavigateToGroup = onNavigateToGroup
        )

        // 请求权限
        Scaffold(
            topBar = { WeTopBar(title = "添加朋友", onBack = onBack) },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            AddFriendContent(
                uiState = uiState,
                innerPadding = innerPadding,
                options = addFriendOptions
            )
        }

        LoadingDialog(uiState.isLoading)
    }
}

/**
 * 添加好友页面内容
 */
@Composable
private fun AddFriendContent(
    uiState: ProfileUiState,
    innerPadding: PaddingValues,
    options: List<AddFriendItem>
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {
        AddFriendOptionsList(options)

        // 只有当二维码生成后才显示
        if (uiState.profile != null && uiState.qrCode.isNotEmpty()) {
            QrCodeSection(
                qrContent = uiState.qrCode,
                wxId = uiState.profile.id,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 添加好友选项列表
 */
@Composable
private fun AddFriendOptionsList(options: List<AddFriendItem>) {
    Column(modifier = Modifier.background(WeTheme.colorScheme.surface)) {
        options.forEachIndexed { index, item ->
            MenuListItem(
                label = item.title,
                description = item.description,
                iconResId = item.iconResId,
                iconColor = item.iconColor,
                height = 68.dp,
                onClick = item.onClick
            )
            if (index < options.lastIndex) {
                WeDivider(modifier = Modifier.padding(start = 58.dp))
            }
        }
    }
}

/**
 * 二维码展示区域
 */
@Composable
private fun QrCodeSection(
    qrContent: String,
    wxId: String,
    modifier: Modifier = Modifier
) {
    val qrCodeState = rememberQRCodeState(
        content = qrContent,
        logoPainter = painterResource(R.drawable.img_logo_outlined),
        backgroundColor = Color.Transparent,
        dotStyle = QrDotStyle.Circle
    )
    val targetWidth = rememberScreenFractionWidth(0.4f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WeQRCode(qrCodeState, modifier = Modifier.size(targetWidth))
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "我的微信号: $wxId",
            fontSize = 15.sp,
            color = WeTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 记忆化添加好友选项
 */
@Composable
private fun rememberAddFriendOptions(
    launchScanner: () -> Unit,
    onNavigateToNFC: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToGroup: () -> Unit
): List<AddFriendItem> {
    return remember(launchScanner, onNavigateToRadar, onNavigateToGroup) {
        listOf(
            AddFriendItem(
                title = "扫一扫",
                iconResId = R.drawable.ic_scan_outlined,
                iconColor = Color(0xFF2B7CF1),
                description = "扫描二维码名片",
                onClick = launchScanner
            ),
            AddFriendItem(
                title = "碰一碰",
                iconResId = R.drawable.ic_nfc_outlined,
                iconColor = Color(0xFF10AEFF),
                description = "通过NFC添加朋友",
                onClick = onNavigateToNFC
            ),
            AddFriendItem(
                title = "雷达",
                iconResId = R.drawable.ic_radar_outlined,
                iconColor = Color(0xFF7468BE),
                description = "添加身边的朋友",
                onClick = onNavigateToRadar
            ),
            AddFriendItem(
                title = "面对面建群",
                iconResId = R.drawable.ic_group_chat_outlined,
                iconColor = Color(0xFF07C160),
                description = "与身边的朋友进入同一个群聊",
                onClick = onNavigateToGroup
            )
        )
    }
}

/**
 * 添加好友选项数据类
 */
data class AddFriendItem(
    val title: String,
    val iconResId: Int,
    val iconColor: Color,
    val description: String,
    val onClick: () -> Unit
)