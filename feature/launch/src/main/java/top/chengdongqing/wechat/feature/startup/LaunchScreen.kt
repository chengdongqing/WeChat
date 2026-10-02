package top.chengdongqing.wechat.feature.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.LocalAppNavigator
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LaunchRoute(
    viewModel: LaunchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalAppNavigator.current

    LaunchScreen(
        state = state,
        onNavigateToHome = {
            navigator.clear()
            navigator.navigateTo(ScreenRoute.Home)
        },
        onNavigateToGuide = {
            navigator.clear()
            navigator.navigateTo(ScreenRoute.Guide)
        }
    )
}

@Composable
fun LaunchScreen(
    state: LaunchUiState = LaunchUiState(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToGuide: () -> Unit = {},
) {
    // 根据登录状态判断将要进入的页面
    LaunchedEffect(state) {
        // 避免启动页闪烁
        delay(500.milliseconds)

        when (state.loginState) {
            LoginState.HasLogin -> onNavigateToHome()
            LoginState.NeedLogin -> onNavigateToGuide()
            else -> Unit
        }
    }

    Image(
        painter = painterResource(R.drawable.img_launch),
        contentDescription = "Launching",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Preview
@Composable
private fun LaunchPreview() {
    WeTheme {
        LaunchScreen()
    }
}