package top.chengdongqing.wechat.features.me.ui.profile.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.util.randomUUID

@Composable
fun EditIDScreen(onBack: () -> Unit) {
    val dialog = rememberDialogState()

    Scaffold(
        topBar = {
            WeTopBar(
                "",
                containerColor = White,
                onBack = onBack
            )
        },
        containerColor = White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Image(
                painter = painterResource(R.drawable.img_logo_gray),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                alpha = 0.15f
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = remember { "微信号：wxid_${randomUUID().take(12)}" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "微信号是账号的唯一凭证，一年不能修改一次。",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
            WeButton(text = "修改微信号", type = ButtonType.Plain) {
                dialog.show(
                    "提示",
                    "在无中心服务器的情况下，为确保当前的设备的唯一性，微信号暂不支持修改",
                    onCancel = null,
                    okText = "我知道了"
                )
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}