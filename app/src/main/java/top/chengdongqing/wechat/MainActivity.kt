package top.chengdongqing.wechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.AppNavigation
import top.chengdongqing.wechat.core.navigation.Screen
import top.chengdongqing.wechat.features.startup.SplashScreen
import top.chengdongqing.wechat.features.startup.StartupState
import top.chengdongqing.wechat.features.startup.StartupViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val startupViewModel: StartupViewModel = hiltViewModel()
            val startupState by startupViewModel.state.collectAsStateWithLifecycle()

            var startDestination by remember { mutableStateOf("") }
            var isSplashTimeout by remember { mutableStateOf(false) }

            // 判断导航目标
            LaunchedEffect(startupState) {
                when (startupState) {
                    is StartupState.ReadyForHome -> startDestination = Screen.HOME
                    is StartupState.NeedWelcome -> startDestination = Screen.WELCOME
                    else -> {}
                }
            }

            WeTheme {
                Crossfade(
                    targetState = startupState,
                    animationSpec = tween(800)
                ) { startupState ->
                    if (isSplashTimeout && startupState !is StartupState.Checking) {
                        AppNavigation(startDestination)
                    } else {
                        SplashScreen(onTimeout = { isSplashTimeout = true })
                    }
                }
            }
        }
    }
}