package top.chengdongqing.wechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.ui.navigation.WeNavigation
import top.chengdongqing.wechat.ui.splash.SplashScreen
import top.chengdongqing.wechat.ui.theme.WeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val showSplash = remember { mutableStateOf(true) }

            WeTheme {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(800)
                ) { isSplashScreen ->
                    if (isSplashScreen.value) {
                        SplashScreen { showSplash.value = false }
                    } else {
                        WeNavigation()
                    }
                }
            }
        }
    }
}