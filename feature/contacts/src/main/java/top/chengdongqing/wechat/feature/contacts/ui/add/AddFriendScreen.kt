package top.chengdongqing.wechat.feature.contacts.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.common.qrcode.generator.QrDotStyle
import top.chengdongqing.wechat.core.common.qrcode.generator.WeQRCode
import top.chengdongqing.wechat.core.common.qrcode.generator.rememberQRCodeState
import top.chengdongqing.wechat.core.common.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.components.menu.WeMenuListItem
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.RequestAddFriendPermission
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.rememberScreenFractionWidth
import top.chengdongqing.wechat.feature.contacts.domain.model.AddContactOption

@Composable
fun AddFriendScreen(
    onBack: () -> Unit,
    onNavigateToNFC: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToGroup: () -> Unit,
    onNavigateToContactDetail: (contactId: String) -> Unit,
    onNavigateToPlainText: (text: String) -> Unit,
    onNavigateToWebView: (url: String) -> Unit,
    viewModel: AddFriendViewModel = hiltViewModel()
) {
    RequestAddFriendPermission(onRevoked = onBack) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        // 生成二维码
        LaunchedEffect(Unit) {
            viewModel.generateMyQrCode()
        }
        // 处理扫码
        val launchScanner = rememberScanCodeLauncher { qrCodes ->
            qrCodes.firstOrNull()?.let(viewModel::handleScannedQrCode)
        }

        // 处理导航事件
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is AddFriendEvent.NavigateToContact -> onNavigateToContactDetail(event.contactId)
                    is AddFriendEvent.ShowText -> onNavigateToPlainText(event.text)
                    is AddFriendEvent.OpenUrl -> onNavigateToWebView(event.url)
                    is AddFriendEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                }
            }
        }

        // 处理点击事件
        val handleAction = { option: AddContactOption ->
            when (option) {
                AddContactOption.Scan -> launchScanner()
                AddContactOption.Nfc -> onNavigateToNFC()
                AddContactOption.Radar -> onNavigateToRadar()
                AddContactOption.FaceToFaceGroup -> onNavigateToGroup()
            }
        }

        Scaffold(
            topBar = {
                WeTopAppBar(
                    title = stringResource(R.string.add_contact_title),
                    onBack = onBack,
                    containerColor = WeTheme.colorScheme.surface
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = WeTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(
                        state = rememberScrollState(),
                        overscrollEffect = rememberBounceOverscrollEffect()
                    )
            ) {
                AddFriendOptionsList(handleAction)

                if (uiState.profile != null && uiState.qrCode.isNotEmpty()) {
                    QrCodeSection(
                        qrContent = uiState.qrCode,
                        myId = uiState.profile!!.id,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        LoadingDialog(uiState.isLoading)
    }
}

@Composable
private fun AddFriendOptionsList(onClick: (AddContactOption) -> Unit) {
    Column {
        AddContactOption.entries.forEach { option ->
            WeMenuListItem(
                label = stringResource(option.titleRes),
                description = stringResource(option.descriptionRes),
                icon = option.icon,
                iconColor = option.iconColor,
                height = 68.dp,
                onClick = { onClick(option) }
            )
            WeDivider(modifier = Modifier.padding(start = 58.dp))
        }
    }
}

@Composable
private fun QrCodeSection(
    qrContent: String,
    myId: String,
    modifier: Modifier = Modifier
) {
    val qrCodeState = rememberQRCodeState(
        content = qrContent,
        logoPainter = painterResource(R.drawable.img_logo_outlined),
        dotStyle = QrDotStyle.Circle
    )
    val targetWidth = rememberScreenFractionWidth(0.4f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(White)
                .padding(12.dp)
        ) {
            WeQRCode(qrCodeState, modifier = Modifier.size(targetWidth))
        }
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = stringResource(R.string.add_contact_my_wechat_id, myId),
            fontSize = 15.sp,
            color = WeTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}
