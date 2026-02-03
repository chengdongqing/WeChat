package top.chengdongqing.wechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.AppNavigation
import top.chengdongqing.wechat.features.startup.SplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            WeTheme {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(800)
                ) { isSplashScreen ->
                    if (isSplashScreen) {
                        // 品牌展示的 Splash，短暂显示后进入导航
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        // 主导航，内部会检查资料状态并路由
                        AppNavigation()
                    }
                }
            }
        }
    }
}