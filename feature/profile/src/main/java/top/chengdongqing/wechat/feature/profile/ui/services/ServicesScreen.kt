package top.chengdongqing.wechat.feature.profile.ui.services

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

data class ServiceItem(
    val name: String,
    @param:DrawableRes val icon: Int,
    val color: Color,
    val onClick: () -> Unit = {}
)

@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onPaymentCode: () -> Unit,
    onWallet: () -> Unit,
    onBills: () -> Unit
) {
    val finance = listOf(
        ServiceItem("信用卡还款", R.drawable.ic_pay_vendor_filled, Color(0xFF3A78D0)),
        ServiceItem("手机充值", R.drawable.ic_call_filled, Color(0xFF23A96E)),
        ServiceItem("理财通", R.drawable.ic_import_export, Color(0xFFE7A536)),
        ServiceItem("保险服务", R.drawable.ic_lock_filled, Color(0xFF3A78D0))
    )
    val life = listOf(
        ServiceItem("生活缴费", R.drawable.ic_pay_vendor_filled, Color(0xFF2F83D0)),
        ServiceItem("城市服务", R.drawable.ic_location_filled, Color(0xFFEC8A32)),
        ServiceItem("医疗健康", R.drawable.ic_favorites_filled, Color(0xFF29A967)),
        ServiceItem("出行服务", R.drawable.ic_radar_outlined, Color(0xFF397ACD)),
        ServiceItem("公益", R.drawable.ic_favorites_filled, Color(0xFFE46B6B)),
        ServiceItem("电影演出", R.drawable.ic_video_filled, Color(0xFF8A67C7)),
        ServiceItem("酒店", R.drawable.ic_location_filled, Color(0xFFDF8D37)),
        ServiceItem("购物", R.drawable.ic_tag_filled, Color(0xFFE25A50))
    )

    Column(Modifier
        .fillMaxSize()
        .background(WeTheme.colorScheme.background)) {
        WeTopAppBar(
            title = "服务",
            onBack = onBack,
            actions = { IconButton(R.drawable.ic_more_outlined, description = "更多") }
        )
        Column(Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF19A76F))
                    .height(116.dp)
            ) {
                ServiceHeroItem(
                    "收付款", "二维码收款与付款", R.drawable.ic_qrcode_outlined,
                    Modifier
                        .weight(1f)
                        .clickable(onClick = onPaymentCode)
                )
                ServiceHeroItem(
                    "钱包", "余额、银行卡与账单", R.drawable.ic_pay_logo_outlined,
                    Modifier
                        .weight(1f)
                        .clickable(onClick = onWallet)
                )
            }
            Spacer(Modifier.height(12.dp))
            ServiceSection("金融理财", finance)
            Spacer(Modifier.height(12.dp))
            ServiceSection("生活服务", life)
            Spacer(Modifier.height(12.dp))
            Text(
                "支付账单",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WeTheme.colorScheme.surface)
                    .clickable(onClick = onBills)
                    .padding(18.dp),
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ServiceHeroItem(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    modifier: Modifier
) {
    Column(
        modifier.padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.White.copy(alpha = .75f), fontSize = 11.sp)
    }
}

@Composable
private fun ServiceSection(title: String, items: List<ServiceItem>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WeTheme.colorScheme.surface)
    ) {
        Text(
            title,
            modifier = Modifier.padding(start = 18.dp, top = 16.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = WeTheme.colorScheme.textPrimary
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(((items.size + 3) / 4 * 92).dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            userScrollEnabled = false
        ) {
            items(items) { item ->
                Column(
                    Modifier
                        .clickable(onClick = item.onClick)
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painterResource(item.icon), null, tint = item.color,
                        modifier = Modifier.size(27.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(item.name, fontSize = 12.sp, color = WeTheme.colorScheme.textPrimary)
                }
            }
        }
    }
}
