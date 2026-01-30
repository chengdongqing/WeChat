package top.chengdongqing.wechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.ui.navigation.WeChatNavigation
import top.chengdongqing.wechat.ui.splash.SplashScreen
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WeChatTheme {
                MainEntry()
            }
        }
    }
}

@Composable
fun MainEntry() {
    var showSplash by remember { mutableStateOf(true) }

    Crossfade(
        targetState = showSplash,
        animationSpec = tween(800)
    ) { isSplashScreen ->
        if (isSplashScreen) {
            SplashScreen(onTimeout = {
                showSplash = false
            })
        } else {
            WeChatNavigation()
        }
    }
}