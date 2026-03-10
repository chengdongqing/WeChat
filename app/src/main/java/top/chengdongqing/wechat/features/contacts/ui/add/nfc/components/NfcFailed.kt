package top.chengdongqing.wechat.features.contacts.ui.add.nfc.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun NfcFailed(reason: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 40.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(WeTheme.colorScheme.danger.copy(alpha = 0.10f))
                .border(2.dp, WeTheme.colorScheme.danger.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close_outlined),
                contentDescription = "失败",
                tint = WeTheme.colorScheme.danger,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = reason,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "请重新靠近对方手机再试",
            fontSize = 13.sp,
            color = WeTheme.colorScheme.textSecondary
        )

        Spacer(Modifier.height(36.dp))

        WeButton(
            text = "重新碰一碰",
            enabled = true,
            onClick = onRetry
        )
    }
}