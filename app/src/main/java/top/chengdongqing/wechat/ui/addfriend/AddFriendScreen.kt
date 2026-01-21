package top.chengdongqing.wechat.ui.addfriend

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

@Composable
fun AddFriendScreen(
    myId: String = "wxid_888888",
    onBack: () -> Unit
) {
    val options = remember {
        listOf(
            AddFriendItem(
                "扫一扫",
                R.drawable.ic_scan_outline,
                Color(0xFF2B7CF1),
                "扫描二维码名片"
            ),
            AddFriendItem(
                "雷达加朋友",
                R.drawable.ic_radar_outline,
                Color(0xFF7468BE),
                "添加身边的朋友"
            ),
            AddFriendItem(
                "面对面建群",
                R.drawable.ic_group_chat_outline,
                Color(0xFF07C160),
                "与身边的朋友进入同一个群聊"
            )
        )
    }

    Scaffold(
        topBar = {
            WeTopBar(title = "添加朋友", onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
        ) {
            Column(modifier = Modifier.background(WeChatTheme.colorScheme.surface)) {
                options.forEachIndexed { index, item ->
                    MenuListItem(
                        title = item.title,
                        description = item.description,
                        iconResId = item.iconResId,
                        iconColor = item.iconColor,
                        height = 68.dp,
                    )
                    if (index < options.lastIndex) {
                        WeDivider(modifier = Modifier.padding(start = 58.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                QrCodeSection(myId)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "我的微信号: $myId",
                    fontSize = 14.sp,
                    color = WeChatTheme.colorScheme.textSecondary
                )
            }
        }
    }
}

@Composable
private fun QrCodeSection(myId: String) {
    val screenWidth = rememberScreenWidth()
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(myId, screenWidth) {
        qrBitmap = generateQrCode(myId, screenWidth, AndroidColor.BLACK).asImageBitmap()
    }

    qrBitmap?.let {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .background(Color.White, RoundedCornerShape(4.dp)) // 增加白色底色和微圆角
                .padding(4.dp)
        ) {
            Image(
                bitmap = it,
                contentDescription = "My QR Code",
                modifier = Modifier.size(screenWidth.dp / LocalDensity.current.density)
            )
        }
    }
}

private suspend fun generateQrCode(content: String, size: Int, color: Int): Bitmap =
    withContext(Dispatchers.Default) {
        val hints = mapOf(EncodeHintType.MARGIN to 0)
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size) { pos ->
            if (bitMatrix.get(pos % size, pos / size)) {
                color
            } else {
                AndroidColor.TRANSPARENT
            }
        }
        Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

@Composable
private fun rememberScreenWidth(fraction: Float = 0.35f): Int {
    val containerSize = LocalWindowInfo.current.containerSize
    return remember {
        (containerSize.width * fraction).roundToInt()
    }
}

private data class AddFriendItem(
    val title: String,
    @get:DrawableRes val iconResId: Int,
    val iconColor: Color,
    val description: String
)