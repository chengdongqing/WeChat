package top.chengdongqing.wechat.ui.addfriend

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.ui.components.qrcode.generator.QrDotStyle
import top.chengdongqing.wechat.ui.components.qrcode.generator.WeQRCode
import top.chengdongqing.wechat.ui.components.qrcode.generator.rememberQRCodeState
import top.chengdongqing.wechat.ui.components.qrcode.scanner.rememberScanCodeLauncher
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.util.rememberScreenFractionWidth

@Composable
fun AddFriendScreen(
    onNavigateToRadar: () -> Unit,
    onNavigateToGroup: () -> Unit,
    onBack: () -> Unit
) {
    val myId = remember { "wxid_${randomUUID().take(12)}" }
    val scanCode = rememberScanCodeLauncher {}

    val options = remember {
        listOf(
            AddFriendItem(
                "扫一扫",
                R.drawable.ic_scan_outlined,
                Color(0xFF2B7CF1),
                "扫描二维码名片"
            ) { scanCode() },
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
                        onTap = item.onTap
                    )
                    if (index < options.lastIndex) {
                        WeDivider(modifier = Modifier.padding(start = 58.dp))
                    }
                }
            }

            QrCodeSection(myId, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QrCodeSection(myId: String, modifier: Modifier) {
    val state = rememberQRCodeState(
        content = myId,
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
            text = "我的微信号: $myId",
            fontSize = 15.sp,
            color = WeChatTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

private data class AddFriendItem(
    val title: String,
    @get:DrawableRes val iconResId: Int,
    val iconColor: Color,
    val description: String,
    val onTap: () -> Unit
)