package top.chengdongqing.wechat.feature.startup

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.util.StatusBarAppearanceEffect
import top.chengdongqing.wechat.core.designsystem.util.onTap

@Composable
fun GuideScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    StatusBarAppearanceEffect(isDark = isSystemInDarkTheme())

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图片
        Image(
            painter = painterResource(id = R.drawable.img_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )

        // 顶部语言切换按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(R.string.welcome_language),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                modifier = Modifier.onTap {
                    onNavigateToLanguage()
                }
            )
        }

        // 底部开始按钮
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(40.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            WeButton(stringResource(R.string.welcome_start)) {
                onNavigateToSetup()
            }
        }
    }
}