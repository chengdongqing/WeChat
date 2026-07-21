package top.chengdongqing.wechat.feature.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.R

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToWelcome: () -> Unit,
    startupViewModel: SplashViewModel = hiltViewModel()
) {
    val startupState by startupViewModel.state.collectAsStateWithLifecycle()

    // 判断导航目标
    LaunchedEffect(startupState) {
        delay(500)

        when (startupState) {
            LoginState.ReadyForHome -> onNavigateToHome()
            LoginState.NeedLogin -> onNavigateToWelcome()
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}