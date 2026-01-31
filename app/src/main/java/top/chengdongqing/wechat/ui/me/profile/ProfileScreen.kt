package top.chengdongqing.wechat.ui.me.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.navigation.Screen
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.theme.White

/**
 * 个人资料页面
 */
@Composable
fun ProfileScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            WeTopBar(
                title = "个人资料",
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = Color(0xFFEDEDED)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                ProfileItem("头像", onClick = {
                    navController.navigate(Screen.Avatar.route)
                }) {
                    AvatarContent()
                }
                ProfileItem("名字") {
                    TextContent("海盐芝士不加糖")
                }
                ProfileItem("性别") {
                    TextContent("男")
                }
                ProfileItem("微信号") {
                    TextContent("wxid_${randomUUID().take(12)}")
                }
                ProfileItem("我的二维码", onClick = {
                    navController.navigate(Screen.QRCode.route)
                }) {
                    QRCodeContent()
                }
                ProfileItem("签名", false) {
                    TextContent("给时光以生命")
                }
            }
            ProfileItem("来电铃声", false) {}
        }
    }
}

/**
 * 个人资料项组件
 */
@Composable
private fun ProfileItem(
    label: String,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.background(White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onClick?.invoke() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧标题
            Text(
                text = label,
                fontSize = 16.sp,
                color = WeChatTheme.colorScheme.textPrimary
            )
            Spacer(modifier = Modifier.widthIn(24.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                content()
            }

            // 右侧箭头
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_right_outlined),
                contentDescription = null,
                tint = WeChatTheme.colorScheme.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        if (showDivider) {
            WeDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}

/**
 * 头像内容
 */
@Composable
private fun AvatarContent() {
    AsyncImage(
        model = R.drawable.img_avatar,
        contentDescription = "头像",
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp)),
        contentScale = ContentScale.Crop
    )
}

/**
 * 二维码内容
 */
@Composable
private fun QRCodeContent() {
    Icon(
        painter = painterResource(R.drawable.ic_qrcode_outlined),
        contentDescription = "二维码",
        modifier = Modifier.size(24.dp),
        tint = WeChatTheme.colorScheme.textSecondary
    )
}

/**
 * 文本内容
 */
@Composable
private fun TextContent(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = WeChatTheme.colorScheme.textSecondary
    )
}