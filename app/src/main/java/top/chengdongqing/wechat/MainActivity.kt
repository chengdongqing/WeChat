package top.chengdongqing.wechat

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.AppNavigation

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // 记录待跳转的路由
    private val navRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 处理冷启动通知
        handleIntent(intent)

        setContent {
            val navController = rememberNavController()
            val route by navRoute

            /**
             * 响应路由事件
             */
            LaunchedEffect(route) {
                route?.let {
                    navController.navigate(it) {
                        launchSingleTop = true
                    }
                    navRoute.value = null
                }
            }

            WeTheme {
                AppNavigation(navController)
            }
        }
    }

    // App已启动后收到通知
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val route = intent?.getStringExtra(EXTRA_ROUTE)
        if (route != null) {
            navRoute.value = route
        }
    }

    companion object {
        const val EXTRA_ROUTE = "extra_route"
    }
}