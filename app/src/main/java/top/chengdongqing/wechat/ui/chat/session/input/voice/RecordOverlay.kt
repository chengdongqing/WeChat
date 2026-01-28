package top.chengdongqing.wechat.ui.chat.session.input.voice

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecordOverlay(status: RecordStatus, amplitude: Float) {
    if (status == RecordStatus.IDLE) return

    // 全屏遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // 声纹动画区
            if (status == RecordStatus.RECORDING) {
                VoiceAmplitudePainter(amplitude)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 底部三个交互点：取消 | 文 | (中间的录音)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 取消按钮
                RecordActionIcon(
                    icon = Icons.Default.Close,
                    label = "取消",
                    isSelected = status == RecordStatus.CANCELING
                )

                // 转文字按钮
                RecordActionIcon(
                    icon = Icons.Default.Menu, // 找个“文”字图标
                    label = "转文字",
                    isSelected = status == RecordStatus.TRANSING
                )
            }
        }
    }
}

@Composable
private fun VoiceAmplitudePainter(amplitude: Float) {
    // 微信风格的声纹是由多个条形组成的动画
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.height(60.dp)
    ) {
        repeat(8) { index ->
            val height by animateFloatAsState(
                targetValue = 10f + (amplitude * (20..50).random()),
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(4.dp)
                    .height(height.dp)
                    .background(Color(0xFF07C160), CircleShape)
            )
        }
    }
}

@Composable
private fun RecordActionIcon(icon: ImageVector, label: String, isSelected: Boolean) {
    val scale by animateFloatAsState(if (isSelected) 1.2f else 1.0f)
    val bgColor =
        if (isSelected) Color(0xFFCC0000).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}