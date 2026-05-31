package top.chengdongqing.wechat

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.common.navigation.AppNavKey
import top.chengdongqing.wechat.core.common.navigation.CommonKey
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.AppNavigation
import top.chengdongqing.wechat.core.network.service.P2PService
import top.chengdongqing.wechat.feature.settings.ui.display.DisplaySettingsViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var json: Json

    // 记录待跳转的路由
    private val pendingNavKey = mutableStateOf<NavKey?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 处理冷启动通知
        handleIntent(intent)
        // 启动前台服务
        initP2PService()

        setContent {
            val backStack = rememberNavBackStack(CommonKey.Splash)
            val navKey by pendingNavKey
            val displayViewModel: DisplaySettingsViewModel = hiltViewModel()
            val displaySettings by displayViewModel.settings.collectAsState()

            // 响应路由事件
            LaunchedEffect(navKey) {
                navKey?.let { key ->
                    if (backStack.last() != key) {
                        backStack.add(key)
                    }
                    pendingNavKey.value = null
                }
            }

            WeTheme(settings = displaySettings) {
                AppNavigation(backStack)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App已启动后收到通知
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val navJson = intent?.getStringExtra(EXTRA_NAV) ?: return

        runCatching {
            json.decodeFromString<AppNavKey>(navJson)
        }.onSuccess { targetNav ->
            pendingNavKey.value = targetNav
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun initP2PService() {
        val intent = Intent(this, P2PService::class.java)
        startForegroundService(intent)
    }

    companion object {
        const val EXTRA_NAV = "extra_nav"
    }
}