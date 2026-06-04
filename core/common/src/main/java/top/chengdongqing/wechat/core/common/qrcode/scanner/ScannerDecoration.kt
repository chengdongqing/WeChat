package top.chengdongqing.wechat.core.common.qrcode.scanner

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
internal fun BoxScope.ScannerDecoration() {
    ScanningAnimation()

    Text(
        text = stringResource(DesignR.string.scan_hint),
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = (-150).dp)
    )
}

@Composable
private fun BoxScope.ScanningAnimation() {
    val screenHeight = LocalWindowInfo.current.containerDpSize.height.value
    val transition = rememberInfiniteTransition(label = "")
    val offsetY by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "QrCodeScanningAnimation"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(x = 0, y = (offsetY * screenHeight).dp.roundToPx())
            }
            .align(Alignment.TopCenter)
            .fillMaxWidth(0.9f)
    ) {
        Image(
            painter = painterResource(DesignR.drawable.img_scan_beam),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
        )
    }
}