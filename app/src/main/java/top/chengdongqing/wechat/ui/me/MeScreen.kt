package top.chengdongqing.wechat.ui.me

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun MeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeChatTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            UserInfoHeader()
            StatusSection()
        }
        ListItem("服务", R.drawable.ic_pay_logo_outline, Color(0xFF07C160))
        Column(modifier = Modifier.background(WeChatTheme.colorScheme.surface)) {
            ListItem("收藏", R.drawable.ic_favorites_outline, Color.Unspecified)
            WeDivider(modifier = Modifier.padding(start = 56.dp))
            ListItem("朋友圈", R.drawable.ic_album_outline, Color(0xFF2782D7))
            WeDivider(modifier = Modifier.padding(start = 56.dp))
            ListItem("表情", R.drawable.ic_sticker_outline, Color(0xFFF9C018))
        }
        ListItem("设置", R.drawable.ic_settings_outline, Color(0xFF2782D7))
    }
}

@Composable
fun UserInfoHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatTheme.colorScheme.surface)
            .padding(start = 24.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.LightGray
        ) {
            Image(
                painter = painterResource(R.drawable.img_avatar),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "海盐芝士不加糖",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = WeChatTheme.colorScheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "微信号：wxid_${randomUUID().take(12)}",
                    fontSize = 14.sp,
                    color = WeChatTheme.colorScheme.textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Icon(
                painter = painterResource(R.drawable.ic_qrcode_outline),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF456F6F)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Icon(
                painter = painterResource(R.drawable.ic_right_outline),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
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

@Composable
fun ListItem(title: String, @DrawableRes iconResId: Int, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatTheme.colorScheme.surface)
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconResId),
            null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_right_outline),
            null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}