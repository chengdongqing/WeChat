package top.chengdongqing.wechat.feature.profile.ui.qrcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.file.createImageUri
import top.chengdongqing.wechat.core.common.qrcode.generator.QRCodeState
import top.chengdongqing.wechat.core.common.qrcode.generator.WeQRCode
import top.chengdongqing.wechat.core.common.qrcode.generator.rememberQRCodeState
import top.chengdongqing.wechat.core.common.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.profile.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastManager
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.LinkBlue
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.window.rememberScreenFractionWidth
import top.chengdongqing.wechat.core.model.UserProfile
import top.chengdongqing.wechat.core.proximity.ui.RequestAddFriendPermission
import top.chengdongqing.wechat.feature.profile.ui.profile.HandleProfileNavigationEvents
import top.chengdongqing.wechat.feature.profile.ui.profile.ProfileViewModel
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun QRCodeScreen(
    onBack: () -> Unit,
    onNavigateToContactDetail: (String) -> Unit,
    onNavigateToPlainText: (String) -> Unit,
    onNavigateToWebView: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    RequestAddFriendPermission(onRevoked = onBack) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        // 生成二维码
        LaunchedEffect(Unit) {
            viewModel.generateQRCode()
        }

        val profile = uiState.profile ?: return@RequestAddFriendPermission
        if (uiState.qrCode.isEmpty()) return@RequestAddFriendPermission

        // 事件处理复用
        HandleProfileNavigationEvents(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            onNavigateToContactDetail = onNavigateToContactDetail,
            onNavigateToPlainText = onNavigateToPlainText,
            onNavigateToWebView = onNavigateToWebView
        )

        val targetWidth = rememberScreenFractionWidth(0.65f)
        val scope = rememberCoroutineScope()

        // QR 码样式状态
        val styleIndex = uiState.qrCodeStyleIndex
            .takeIf { it in QR_CODE_STYLES.indices }
            ?: 0
        val qrCodeState = rememberQRCodeState(
            content = uiState.qrCode,
            logoPainter = painterResource(DesignR.drawable.img_logo_outlined),
            brush = QR_CODE_STYLES[styleIndex]
        )
        LaunchedEffect(styleIndex, qrCodeState) {
            qrCodeState.brush = QR_CODE_STYLES[styleIndex]
        }

        // 准备生成图片所需的资源
        val context = LocalContext.current
        val density = LocalDensity.current
        val avatarBitmap = rememberAvatarBitmap(profile.avatarPath)
            ?: return@RequestAddFriendPermission
        val textMeasurer = rememberTextMeasurer()

        val cardRenderer = remember(profile, qrCodeState, avatarBitmap) {
            QrCardRenderer(profile, qrCodeState, avatarBitmap, textMeasurer, context)
        }

        Scaffold(
            topBar = { WeTopAppBar(onBack = onBack) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = WeTheme.colorScheme.background
        ) { innerPadding ->
            QRCodeContent(
                profile = profile,
                qrCodeState = qrCodeState,
                targetWidth = targetWidth,
                innerPadding = innerPadding,
                onScanQRCode = viewModel::handleScannedQRCode,
                onChangeStyle = {
                    viewModel.selectNextQrCodeStyle(QR_CODE_STYLES.size)
                },
                onSaveToAlbum = {
                    handleSaveToAlbum(
                        cardRenderer = cardRenderer,
                        density = density,
                        context = context,
                        scope = scope,
                        viewModel = viewModel
                    )
                }
            )
        }

        LoadingDialog(uiState.isLoading)
    }
}

/**
 * QR 码内容区域
 */
@Composable
private fun QRCodeContent(
    profile: UserProfile,
    qrCodeState: QRCodeState,
    targetWidth: Dp,
    innerPadding: PaddingValues,
    onScanQRCode: (String) -> Unit,
    onChangeStyle: () -> Unit,
    onSaveToAlbum: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .width(targetWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileBar(profile)
            Spacer(modifier = Modifier.height(28.dp))
            WeQRCode(qrCodeState)
            Spacer(modifier = Modifier.height(28.dp))
            QRCodeHintText()
        }

        QRCodeFooter(
            onScanQRCode = onScanQRCode,
            onChangeStyle = onChangeStyle,
            onSaveToAlbum = onSaveToAlbum
        )

        Spacer(modifier = Modifier.height(42.dp))
    }
}

/**
 * 个人信息栏
 */
@Composable
private fun ProfileBar(profile: UserProfile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = profile.avatarPath,
            contentDescription = "头像",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        ProfileInfo(profile)
    }
}

/**
 * 个人信息文本
 */
@Composable
private fun ProfileInfo(profile: UserProfile) {
    Column {
        Text(
            text = profile.nickname,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = WeTheme.colorScheme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        profile.signature?.takeIf { it.isNotBlank() }?.let { signature ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = signature,
                fontSize = 12.sp,
                color = WeTheme.colorScheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * QR 码提示文本
 */
@Composable
private fun QRCodeHintText() {
    Text(
        text = stringResource(R.string.me_qrcode_hint),
        fontSize = 12.sp,
        color = WeTheme.colorScheme.textSecondary,
        textAlign = TextAlign.Center
    )
}

/**
 * QR 码底部操作栏
 */
@Composable
private fun QRCodeFooter(
    onScanQRCode: (String) -> Unit,
    onChangeStyle: () -> Unit,
    onSaveToAlbum: () -> Unit
) {
    val launchScanner = rememberScanCodeLauncher { qrCodes ->
        onScanQRCode(qrCodes.first())
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinkText(
            text = stringResource(R.string.me_qrcode_scan),
            onClick = launchScanner
        )
        FooterDivider()
        LinkText(
            text = stringResource(R.string.me_qrcode_change_style),
            onClick = onChangeStyle
        )
        FooterDivider()
        LinkText(
            text = stringResource(R.string.me_qrcode_save),
            onClick = onSaveToAlbum
        )
    }
}

/**
 * 记忆化头像 Bitmap
 */
@Composable
fun rememberAvatarBitmap(path: String?): Bitmap? {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(path) {
        if (path.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }

        val loadedBitmap = withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(path)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        bitmap = loadedBitmap
    }

    return bitmap
}

/**
 * 处理保存到相册操作
 */
private fun handleSaveToAlbum(
    cardRenderer: QrCardRenderer,
    density: Density,
    context: Context,
    scope: CoroutineScope,
    viewModel: ProfileViewModel
) {
    ToastManager.loading(context.getString(R.string.msg_processing))

    scope.launch {
        try {
            val bitmap = cardRenderer.generateBitmap(density = density)
            val uri = context.createImageUri(bitmap)
            val success = viewModel.saveImage(uri)

            delay(500.milliseconds)

            ToastManager.show(
                title = if (success) {
                    context.getString(DesignR.string.msg_save_success)
                } else {
                    context.getString(DesignR.string.msg_save_failed)
                },
                icon = if (success) ToastIcon.Success else ToastIcon.Fail
            )
        } catch (e: Exception) {
            ToastManager.fail("${context.getString(DesignR.string.msg_save_failed)}: ${e.message}")
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = LinkBlue,
        modifier = Modifier.onTap(onClick = onClick)
    )
}

@Composable
private fun FooterDivider() {
    WeDivider(
        modifier = Modifier.height(8.dp),
        orientation = Orientation.Vertical
    )
}

/**
 * 二维码预设样式
 */
private val QR_CODE_STYLES = listOf(
    SolidColor(Color(0xFF222222)),                   // 黑色
    SolidColor(Color(0xFF00C35A)),                   // 微信绿
    Brush.linearGradient(                                   // 红→紫
        colors = listOf(Color(0xFFE94E3E), Color(0xFF8D46FB)),
        start = Offset.Zero,
        end = Offset.Infinite
    ),
    Brush.linearGradient(                                   // 蓝→青
        colors = listOf(Color(0xFF1989FA), Color(0xFF00C35A)),
        start = Offset.Zero,
        end = Offset.Infinite
    ),
    Brush.linearGradient(                                   // 橙→红
        colors = listOf(Color(0xFFFF9800), Color(0xFFE94E3E)),
        start = Offset.Zero,
        end = Offset.Infinite
    ),
    Brush.linearGradient(                                   // 紫→蓝
        colors = listOf(Color(0xFF8D46FB), Color(0xFF1989FA)),
        start = Offset.Zero,
        end = Offset.Infinite
    )
)
