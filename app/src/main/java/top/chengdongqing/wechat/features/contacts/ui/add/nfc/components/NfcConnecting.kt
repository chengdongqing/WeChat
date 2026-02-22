package top.chengdongqing.wechat.features.contacts.ui.add.nfc.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.theme.GreenPrimary

@Composable
fun NfcConnecting() {
    val infiniteTransition = rememberInfiniteTransition(label = "connecting")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 40.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(GreenPrimary.copy(alpha = 0.08f))
                .border(2.dp, GreenPrimary.copy(alpha = alpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            WeLoading(
                size = 42.dp,
                color = GreenPrimary.copy(alpha = alpha)
            )
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text = "正在获取对方信息...",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "请保持手机靠近",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}