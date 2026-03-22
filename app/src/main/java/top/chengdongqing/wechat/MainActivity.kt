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
import top.chengdongqing.wechat.data.network.service.P2PService

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // 记录待跳转的路由
    private val navRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 处理冷启动通知
        handleIntent(intent)
        // 启动前台服务
        initP2PService()

        setContent {
            val navController = rememberNavController()
            val route by navRoute

            // 响应路由事件
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App已启动后收到通知
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val route = intent?.getStringExtra(EXTRA_ROUTE)
        if (route != null) {
            navRoute.value = route
        }
    }

    private fun initP2PService() {
        val intent = Intent(this, P2PService::class.java)
        startForegroundService(intent)
    }

    companion object {
        const val EXTRA_ROUTE = "extra_route"
    }
}