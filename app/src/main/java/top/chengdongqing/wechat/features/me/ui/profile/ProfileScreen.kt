package top.chengdongqing.wechat.features.me.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.features.me.navigation.MeRoute

/**
 * 个人资料页面
 */
@Composable
fun ProfileScreen(navController: NavController) {
    Scaffold(
        topBar = {
            WeTopBar(
                title = "个人资料",
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                ProfileItem("头像", onClick = {
                    navController.navigate(MeRoute.Edit.AVATAR)
                }) {
                    AvatarContent()
                }
                ProfileItem("名字", onClick = {
                    navController.navigate(MeRoute.Edit.NAME)
                }) {
                    TextContent("海盐芝士不加糖")
                }
                ProfileItem("性别", onClick = {
                    navController.navigate(MeRoute.Edit.GENDER)
                }) {
                    TextContent("男")
                }
                ProfileItem("微信号", onClick = {
                    navController.navigate(MeRoute.Edit.ID)
                }) {
                    TextContent("wxid_${randomUUID().take(12)}")
                }
                ProfileItem("我的二维码", onClick = {
                    navController.navigate(MeRoute.QR_CODE)
                }) {
                    QRCodeContent()
                }
                ProfileItem(
                    label = "签名",
                    showDivider = false,
                    onClick = {
                        navController.navigate(MeRoute.Edit.SIGNATURE)
                    }
                ) {
                    TextContent("这么近 那么美")
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
        MenuListItem(label, content = content, onClick = onClick)

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
        tint = WeTheme.colorScheme.textSecondary
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
        color = WeTheme.colorScheme.textSecondary
    )
}