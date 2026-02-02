package top.chengdongqing.wechat.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.util.StatusBarAppearanceEffect
import top.chengdongqing.wechat.ui.util.weClickable

@Composable
fun WelcomeScreen(onNavigateToSetup: () -> Unit = {}) {
    StatusBarAppearanceEffect(isDark = isSystemInDarkTheme())

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "语言",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                modifier = Modifier.weClickable { }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(40.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            WeButton("开始使用") {
                onNavigateToSetup()
            }
        }
    }
}