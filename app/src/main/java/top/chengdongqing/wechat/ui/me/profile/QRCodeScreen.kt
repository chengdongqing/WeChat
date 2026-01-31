package top.chengdongqing.wechat.ui.me.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.ui.components.qrcode.generator.WeQRCode

@Composable
fun QRCodeScreen() {
    val purpleGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFE94E3E), Color(0xFF8D46FB)), // 红到紫
        start = Offset.Zero,
        end = Offset.Infinite
    )

    WeQRCode(
        "wxid_${randomUUID()}",
        logoPainter = painterResource(R.drawable.img_logo_outlined),
        brush = purpleGradient,
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = 100.dp)
    )
}

@Preview
@Composable
private fun Preview() {
    QRCodeScreen()
}