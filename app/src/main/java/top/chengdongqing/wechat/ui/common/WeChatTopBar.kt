package top.chengdongqing.wechat.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun WeChatTopBar() {
    Surface(
        color = WeChatTheme.colorScheme.backgroundDefault,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding() // 自动处理系统顶栏高度
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back_outline),
                contentDescription = "返回",
                modifier = Modifier.size(24.dp),
                tint = WeChatTheme.colorScheme.textPrimary
            )
            Text(
                text = "微信",
                fontSize = 16.sp,
                color = WeChatTheme.colorScheme.textPrimary
            )
            Icon(
                painter = painterResource(R.drawable.ic_search_outline),
                contentDescription = "搜索",
                modifier = Modifier.size(24.dp),
                tint = WeChatTheme.colorScheme.textPrimary
            )
            Icon(
                painter = painterResource(R.drawable.ic_plus_circle_outline),
                contentDescription = "更多",
                modifier = Modifier.size(24.dp),
                tint = WeChatTheme.colorScheme.textPrimary
            )
        }
    }
}