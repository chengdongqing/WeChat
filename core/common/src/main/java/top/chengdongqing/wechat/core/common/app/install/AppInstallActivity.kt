package top.chengdongqing.wechat.core.common.app.install

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.util.showToast
import java.io.File

/**
 * 承接未知来源授权并启动安装 Session。系统安装确认页仍由 Android 提供。
 */
class AppInstallActivity : ComponentActivity() {
    private var installStarted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (packageManager.canRequestPackageInstalls()) {
            startInstall()
        } else {
            showToast("未允许安装未知应用")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (packageManager.canRequestPackageInstalls()) {
            startInstall()
        } else {
            permissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun startInstall() {
        if (installStarted) return
        installStarted = true
        val path = intent.getStringExtra(AppPackageInstaller.EXTRA_FILE_PATH)
        if (path == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            runCatching {
                AppPackageInstaller.install(this@AppInstallActivity, File(path))
            }.onFailure {
                showToast(it.message ?: "无法安装此应用")
            }
            finish()
        }
    }
}
