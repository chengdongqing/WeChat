package top.chengdongqing.wechat.feature.startup

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.LocalAppNavigator
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.launch.R as LaunchR

@Composable
fun GuideRoute() {
    val navigator = LocalAppNavigator.current

    GuideScreen(
        onNavigateToLogin = {
            navigator.navigateTo(ScreenRoute.Login)
        },
        onNavigateToLanguage = {
            navigator.navigateTo(ScreenRoute.LanguageSettings)
        }
    )
}

@Composable
fun GuideScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
) {
    WeTheme(isDark = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图
            Image(
                painter = painterResource(R.drawable.img_launch),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 顶部语言切换入口
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(LaunchR.string.action_language),
                    color = WeTheme.colorScheme.textPrimary,
                    style = WeTheme.typography.bodyLarge,
                    modifier = Modifier.onTap(onClick = onNavigateToLanguage)
                )
            }

            // 底部登录入口
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                WeButton(stringResource(LaunchR.string.action_login)) {
                    onNavigateToLogin()
                }
            }
        }
    }
}

@Preview
@Composable
private fun GuidePreview() {
    GuideScreen()
}