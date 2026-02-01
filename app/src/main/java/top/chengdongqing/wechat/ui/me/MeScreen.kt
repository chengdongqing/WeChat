package top.chengdongqing.wechat.ui.me

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.navigation.NavController
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.ui.navigation.Screen
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.util.weClickable

@Composable
fun MeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(WeChatTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            UserInfoSection(
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToQRCode = { navController.navigate(Screen.QRCode.route) }
            )
            StatusSection()
        }
        MenuListItem("服务", R.drawable.ic_pay_logo_outlined, Color(0xFF07C160))
        Column(modifier = Modifier.background(WeChatTheme.colorScheme.surface)) {
            MenuListItem("收藏", R.drawable.ic_favorites_outlined, Color.Unspecified)
            WeDivider(modifier = Modifier.padding(start = 56.dp))
            MenuListItem("朋友圈", R.drawable.ic_album_outlined, Color(0xFF2782D7))
            WeDivider(modifier = Modifier.padding(start = 56.dp))
            MenuListItem("表情", R.drawable.ic_emoji_outlined, Color(0xFFF9C018))
        }
        MenuListItem("设置", R.drawable.ic_settings_outlined, Color(0xFF2782D7))
    }
}

@Composable
fun UserInfoSection(
    onNavigateToProfile: () -> Unit,
    onNavigateToQRCode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatTheme.colorScheme.surface)
            .weClickable { onNavigateToProfile() }
            .padding(start = 24.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.img_avatar),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "海盐芝士不加糖",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeChatTheme.colorScheme.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 20.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_qrcode_outlined),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .weClickable { onNavigateToQRCode() },
                    tint = Color(0xFF456F6F)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "微信号：wxid_${randomUUID().take(12)}",
                    fontSize = 14.sp,
                    color = WeChatTheme.colorScheme.textSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 20.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_right_outlined),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = 4.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StatusSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.width(84.dp))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .border(0.5.dp, WeChatTheme.colorScheme.divider, CircleShape)
                .clickable { }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            Text("状态", fontSize = 12.sp, color = Color.Gray)
        }
    }
}