package top.chengdongqing.wechat.features.me.ui.qrcode

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.WeQRCode
import top.chengdongqing.wechat.core.designsystem.components.qrcode.generator.rememberQRCodeState
import top.chengdongqing.wechat.core.designsystem.components.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.rememberToastState
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.LinkColor
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.designsystem.util.rememberScreenFractionWidth
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.util.createImageUri
import top.chengdongqing.wechat.core.util.saveToAlbum
import top.chengdongqing.wechat.data.model.UserProfile
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel
import kotlin.time.Duration

@Composable
fun QRCodeScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.profile == null) return
    val profile = uiState.profile!!

    val targetWidth = rememberScreenFractionWidth(0.65f)
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val toast = rememberToastState()

    // 样式循环切换
    var styleIndex by remember { mutableIntStateOf(0) }

    val state = rememberQRCodeState(
        content = profile.id,
        logoPainter = painterResource(R.drawable.img_logo_outlined),
        brush = QR_CODE_STYLES[styleIndex],
        backgroundColor = Color.Transparent
    )

    val resources = LocalResources.current
    val avatarBitmap = remember {
        ResourcesCompat.getDrawable(resources, R.drawable.img_avatar, null)!!.toBitmap()
    }
    val textMeasurer = rememberTextMeasurer()

    // 用于生成图片
    val cardRenderer = remember(profile, state, avatarBitmap) {
        QrCardRenderer(profile, state, avatarBitmap, textMeasurer)
    }

    Scaffold(
        topBar = {
            WeTopBar("", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
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
                WeQRCode(state)
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "扫一扫上面的二维码图案，加我为朋友。",
                    fontSize = 12.sp,
                    color = WeTheme.colorScheme.textSecondary
                )
            }

            FooterBar(
                onChangeStyle = {
                    styleIndex = (styleIndex + 1) % QR_CODE_STYLES.size
                    state.brush = QR_CODE_STYLES[styleIndex]
                },
                onSaveToAlbum = {
                    toast.show(
                        title = "正在处理...",
                        icon = ToastIcon.Loading,
                        duration = Duration.INFINITE,
                        mask = true
                    )

                    scope.launch {
                        val bitmap = cardRenderer.generateBitmap(density = density)
                        val uri = context.createImageUri(bitmap)
                        val success = context.saveToAlbum(uri)

                        delay(200)
                        toast.hide()
                        delay(200)
                        toast.show(
                            title = if (success) "已保存到相册" else "保存失败",
                            icon = if (success) ToastIcon.Success else ToastIcon.Fail
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(42.dp))
        }
    }
}

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
        Column {
            Text(
                text = profile.nickname,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = WeTheme.colorScheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (profile.signature?.isNotBlank().isTrue()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile.signature!!,
                    fontSize = 12.sp,
                    color = WeTheme.colorScheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FooterBar(onChangeStyle: () -> Unit, onSaveToAlbum: () -> Unit) {
    val launchScanner = rememberScanCodeLauncher {}

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinkText("扫一扫", launchScanner)
        FooterDivider()
        LinkText("换个样式", onChangeStyle)
        FooterDivider()
        LinkText("保存图片", onSaveToAlbum)
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = LinkColor,
        modifier = Modifier.weClickable(onClick = onClick)
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
    SolidColor(Color(0xFF222222)),                          // 黑色
    SolidColor(Color(0xFF00C35A)),                          // 微信绿
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