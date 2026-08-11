package top.chengdongqing.wechat.feature.profile.ui.services

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.profile.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import java.security.SecureRandom

private val PayGreen = Color(0xFF17AD73)

@Composable
fun PaymentCodeScreen(onBack: () -> Unit) {
    var receiveMode by remember { mutableStateOf(false) }
    var generation by remember { mutableIntStateOf(0) }
    val token = remember(generation, receiveMode) {
        createPaymentToken(if (receiveMode) "receive" else "pay")
    }

    LaunchedEffect(generation, receiveMode) {
        delay(60_000)
        generation++
    }

    Column(Modifier
        .fillMaxSize()
        .background(PayGreen)) {
        WeTopAppBar(
            title = if (receiveMode) "二维码收款" else "向商家付款",
            containerColor = PayGreen,
            contentColor = Color.White,
            onBack = onBack,
            actions = { IconButton(DesignR.drawable.ic_more_outlined, description = "更多") }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ModeButton("付款码", !receiveMode) { receiveMode = false }
            ModeButton("收款码", receiveMode) { receiveMode = true }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            if (receiveMode) {
                ReceiveCodeContent(token) { generation++ }
            } else {
                PayCodeContent(token) { generation++ }
            }
            Text(
                "离线演示码，不代表真实货币",
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color.White.copy(alpha = .2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 25.dp, vertical = 8.dp)
    )
}

@Composable
private fun PayCodeContent(token: String, onRefresh: () -> Unit) {
    val number = remember(token) { token.filter(Char::isDigit).takeLast(18).padStart(18, '0') }
    val barcode = remember(token) { encodeBitmap(number, BarcodeFormat.CODE_128, 900, 230) }
    val qrCode = remember(token) { encodeBitmap(token, BarcodeFormat.QR_CODE, 560, 560) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("请使用扫码设备扫描付款码", color = Color(0xFF666666), fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Image(
            barcode.asImageBitmap(), "付款条形码",
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        )
        Text(
            number.chunked(4).joinToString(" "),
            fontSize = 16.sp,
            letterSpacing = 2.sp,
            color = Color(0xFF333333)
        )
        Spacer(Modifier.height(22.dp))
        Image(qrCode.asImageBitmap(), "付款二维码", modifier = Modifier.size(224.dp))
        Spacer(Modifier.height(18.dp))
        RefreshCode(onRefresh)
        Spacer(Modifier.height(22.dp))
        PaymentMethodRow()
    }
}

@Composable
private fun ReceiveCodeContent(token: String, onRefresh: () -> Unit) {
    val qrCode = remember(token) {
        encodeBitmap(token.replace("/pay/", "/receive/"), BarcodeFormat.QR_CODE, 700, 700)
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("使用扫码功能向我付款", color = Color(0xFF666666), fontSize = 14.sp)
        Spacer(Modifier.height(26.dp))
        Image(qrCode.asImageBitmap(), "收款二维码", modifier = Modifier.size(270.dp))
        Spacer(Modifier.height(20.dp))
        Text("设置金额", color = PayGreen, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(22.dp))
        RefreshCode(onRefresh)
        Spacer(Modifier.height(28.dp))
        Text("收款记录", color = Color(0xFF555555), fontSize = 15.sp)
    }
}

@Composable
private fun RefreshCode(onRefresh: () -> Unit) {
    Row(
        Modifier
            .clickable(onClick = onRefresh)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(DesignR.drawable.ic_backup_restore), null,
            tint = PayGreen, modifier = Modifier.size(18.dp)
        )
        Text(" 刷新二维码", color = PayGreen, fontSize = 14.sp)
    }
}

@Composable
private fun PaymentMethodRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF7F7F7))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(DesignR.drawable.ic_pay_logo_outlined), null,
            tint = PayGreen, modifier = Modifier.size(24.dp)
        )
        Column(Modifier
            .weight(1f)
            .padding(horizontal = 12.dp)) {
            Text("零钱", fontSize = 15.sp, color = Color(0xFF222222))
            Text("优先使用此支付方式", fontSize = 11.sp, color = Color(0xFF999999))
        }
        Icon(
            painterResource(DesignR.drawable.ic_right_outlined), null,
            tint = Color(0xFFAAAAAA), modifier = Modifier.size(18.dp)
        )
    }
}

private fun createPaymentToken(mode: String): String {
    val bytes = ByteArray(16).also(SecureRandom()::nextBytes)
    val nonce = bytes.joinToString("") { "%02x".format(it) }
    return "wechat-offline://wallet/$mode/$nonce/${System.currentTimeMillis()}"
}

private fun encodeBitmap(
    value: String,
    format: BarcodeFormat,
    width: Int,
    height: Int
): Bitmap {
    val hints = if (format == BarcodeFormat.QR_CODE) {
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
    } else {
        mapOf(EncodeHintType.MARGIN to 8)
    }
    val matrix = MultiFormatWriter().encode(value, format, width, height, hints)
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (matrix[x, y]) android.graphics.Color.BLACK
            else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
