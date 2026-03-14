package top.chengdongqing.wechat.features.chat.ui.session.peer.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun OwnerWaitingView() {
    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.WifiTethering,
            contentDescription = null,
            tint = WeTheme.colorScheme.primary,
            modifier = Modifier
                .size(56.dp)
                .scale(scale),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.conn_status_group_created),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
        Text(
            text = stringResource(R.string.conn_desc_group_created_hint),
            fontSize = 13.sp,
            color = WeTheme.colorScheme.textSecondary
        )
    }
}
