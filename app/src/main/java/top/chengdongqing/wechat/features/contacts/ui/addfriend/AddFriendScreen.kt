package top.chengdongqing.wechat.features.contacts.ui.addfriend

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.QrDotStyle
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.WeQRCode
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.rememberQRCodeState
import top.chengdongqing.wechat.core.designsystem.components.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberScreenFractionWidth
import top.chengdongqing.wechat.core.util.showToast

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddFriendScreen(
    onNavigateToRadar: () -> Unit,
    onNavigateToGroup: () -> Unit,
    onNavigateToContactDetail: (contactId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: AddFriendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 处理扫码
    val launchScanner = rememberScanCodeLauncher { qrCodes ->
        viewModel.handleScannedQRCode(qrCodes.first())
    }

    // 处理导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is AddFriendNavigationEvent.NavigateToContactDetail -> {
                    onNavigateToContactDetail(event.contactId)
                }
            }
        }
    }

    // 显示错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val options = remember {
        listOf(
            AddFriendItem(
                "扫一扫",
                R.drawable.ic_scan_outlined,
                Color(0xFF2B7CF1),
                "扫描二维码名片"
            ) { launchScanner() },
            AddFriendItem(
                "雷达",
                R.drawable.ic_radar_outlined,
                Color(0xFF7468BE),
                "添加身边的朋友"
            ) { onNavigateToRadar() },
            AddFriendItem(
                "面对面建群",
                R.drawable.ic_group_chat_outlined,
                Color(0xFF07C160),
                "与身边的朋友进入同一个群聊"
            ) { onNavigateToGroup() }
        )
    }

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    val context = LocalContext.current
    val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    LaunchedEffect(bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            context.showToast("此设备不支持蓝牙")
        } else if (!bluetoothAdapter.isEnabled) {
            context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        permissionState.launchMultiplePermissionRequest()
    }

    Scaffold(
        topBar = {
            WeTopBar(title = "添加朋友", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(Color(0xFFF7F7F7))
            ) {
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

                // 只有当二维码生成后才显示
                if (uiState.qrCode.isNotEmpty()) {
                    QrCodeSection(
                        qrContent = uiState.qrCode,
                        wxId = uiState.wxId,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Loading 遮罩
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun QrCodeSection(
    qrContent: String,
    wxId: String,
    modifier: Modifier = Modifier
) {
    val state = rememberQRCodeState(
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
        WeQRCode(state, modifier = Modifier.size(targetWidth))
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "我的微信号: $wxId",
            fontSize = 15.sp,
            color = WeTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

private data class AddFriendItem(
    val title: String,
    @get:DrawableRes val iconResId: Int,
    val iconColor: Color,
    val description: String,
    val onClick: () -> Unit
)