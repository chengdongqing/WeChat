package top.chengdongqing.wechat.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White

@Composable
fun PlainTextScreen(
    text: String,
    onBack: () -> Unit
) {
    Scaffold(topBar = {
        WeTopBar(onBack = onBack, backIconResId = R.drawable.ic_close_outlined)
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(innerPadding)
        ) {
            Text(text = text, color = WeTheme.colorScheme.textPrimary, fontSize = 15.sp)
        }
    }
}