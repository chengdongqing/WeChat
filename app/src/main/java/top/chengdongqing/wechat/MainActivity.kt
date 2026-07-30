package top.chengdongqing.wechat

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Trace
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
import top.chengdongqing.wechat.core.common.qrcode.scanner.QRCodeScannerActivity
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.navigation.AppNavigation
import top.chengdongqing.wechat.core.navigation.NavigationKey
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

        handleIntent(intent)
        initP2PService()
        publishAppShortcuts()

        setContent {
            val backStack = rememberNavBackStack(NavigationKey.Splash)
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

            // Provides a consistent user-visible startup endpoint for Macrobenchmark.
            LaunchedEffect(Unit) {
                reportFullyDrawn()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val navJson = intent?.getStringExtra(EXTRA_NAV) ?: return

        runCatching {
            json.decodeFromString<NavigationKey>(navJson)
        }.onSuccess { targetNav ->
            pendingNavKey.value = targetNav
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun initP2PService() {
        Trace.beginSection("P2PService.start")
        try {
            startForegroundService(Intent(this, P2PService::class.java))
        } finally {
            Trace.endSection()
        }
    }

    /**
     * 注册系统桌面长按菜单。动态快捷方式可以直接携带现有 NavigationKey，
     * 因而无需为快捷入口维护另一套页面或深链路由。
     */
    private fun publishAppShortcuts() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { action = Intent.ACTION_MAIN }
            ?: Intent(this, MainActivity::class.java).setAction(Intent.ACTION_MAIN)

        val paymentNav = json.encodeToString<NavigationKey>(NavigationKey.PaymentCode)
        val paymentShortcut = ShortcutInfo.Builder(this, SHORTCUT_PAYMENT)
            .setShortLabel("收付款")
            .setLongLabel("收付款")
            .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_payment))
            .setIntent(
                Intent(this, MainActivity::class.java).apply {
                    action = ACTION_SHORTCUT_PAYMENT
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_NAV, paymentNav)
                }
            )
            .setRank(0)
            .build()

        val scanShortcut = ShortcutInfo.Builder(this, SHORTCUT_SCAN)
            .setShortLabel("扫一扫")
            .setLongLabel("扫一扫")
            .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_scan))
            // 先建立应用任务栈，再把扫描页放到栈顶；退出扫描后自然回到微信。
            .setIntents(
                arrayOf(
                    launchIntent,
                    QRCodeScannerActivity.newIntent(this).setAction(ACTION_SHORTCUT_SCAN)
                )
            )
            .setRank(1)
            .build()

        val qrCodeNav = json.encodeToString<NavigationKey>(NavigationKey.QrCode)
        val myQrCodeShortcut = ShortcutInfo.Builder(this, SHORTCUT_MY_QR_CODE)
            .setShortLabel("我的二维码")
            .setLongLabel("我的二维码")
            .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_qrcode))
            .setIntent(
                Intent(this, MainActivity::class.java).apply {
                    action = ACTION_SHORTCUT_MY_QR_CODE
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_NAV, qrCodeNav)
                }
            )
            .setRank(2)
            .build()

        runCatching {
            shortcutManager.dynamicShortcuts = listOf(
                paymentShortcut,
                scanShortcut,
                myQrCodeShortcut
            )
        }
    }

    companion object {
        const val EXTRA_NAV = "extra_nav"
        private const val SHORTCUT_SCAN = "scan"
        private const val SHORTCUT_MY_QR_CODE = "my_qr_code"
        private const val SHORTCUT_PAYMENT = "payment"
        private const val ACTION_SHORTCUT_SCAN =
            "top.chengdongqing.wechat.action.SHORTCUT_SCAN"
        private const val ACTION_SHORTCUT_MY_QR_CODE =
            "top.chengdongqing.wechat.action.SHORTCUT_MY_QR_CODE"
        private const val ACTION_SHORTCUT_PAYMENT =
            "top.chengdongqing.wechat.action.SHORTCUT_PAYMENT"
    }
}
