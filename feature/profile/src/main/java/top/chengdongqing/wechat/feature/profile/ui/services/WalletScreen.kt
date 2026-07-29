package top.chengdongqing.wechat.feature.profile.ui.services

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun WalletScreen(
    onBack: () -> Unit,
    onBalance: () -> Unit,
    onCards: () -> Unit,
    onBills: () -> Unit
) {
    Column(Modifier
        .fillMaxSize()
        .background(WeTheme.colorScheme.background)) {
        WeTopAppBar(
            title = "钱包",
            onBack = onBack,
            actions = { IconButton(R.drawable.ic_more_outlined, description = "更多") }
        )
        Column(
            Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF19A76F))
                    .padding(vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painterResource(R.drawable.ic_pay_logo_outlined), null,
                    tint = Color.White, modifier = Modifier.size(42.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text("零钱", color = Color.White.copy(alpha = .82f), fontSize = 14.sp)
                Text(
                    "¥ 0.00",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "查看余额",
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = .16f))
                        .clickable(onClick = onBalance)
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                )
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(WeTheme.colorScheme.surface)
            ) {
                WalletRow("银行卡", "添加银行卡", R.drawable.ic_pay_vendor_filled, onCards)
                WalletRow("支付账单", "查看全部交易记录", R.drawable.ic_file_filled, onBills)
                WalletRow("亲属卡", "为家人提供支付额度", R.drawable.ic_person_filled)
                WalletRow("支付分", "先享后付，信用生活", R.drawable.ic_check)
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(WeTheme.colorScheme.surface)
            ) {
                WalletRow("消费者保护", "支付安全与服务保障", R.drawable.ic_lock_filled)
                WalletRow("支付设置", "实名认证、扣款顺序", R.drawable.ic_settings_outlined)
            }
        }
    }
}

@Composable
private fun WalletRow(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(icon), null, tint = Color(0xFF19A76F),
            modifier = Modifier.size(25.dp)
        )
        Column(Modifier
            .weight(1f)
            .padding(horizontal = 14.dp)) {
            Text(title, fontSize = 16.sp, color = WeTheme.colorScheme.textPrimary)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, fontSize = 12.sp, color = WeTheme.colorScheme.textSecondary)
        }
        Icon(
            painterResource(R.drawable.ic_right_outlined), null,
            tint = WeTheme.colorScheme.textSecondary, modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun WalletSubScreen(
    title: String,
    description: String,
    onBack: () -> Unit
) {
    Column(Modifier
        .fillMaxSize()
        .background(WeTheme.colorScheme.background)) {
        WeTopAppBar(title = title, onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(WeTheme.colorScheme.surface)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painterResource(R.drawable.ic_pay_logo_outlined), null,
                tint = Color(0xFF19A76F), modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(description, color = WeTheme.colorScheme.textSecondary, fontSize = 14.sp)
        }
    }
}
