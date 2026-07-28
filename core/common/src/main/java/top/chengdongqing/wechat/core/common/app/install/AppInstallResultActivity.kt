package top.chengdongqing.wechat.core.common.app.install

import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import androidx.activity.ComponentActivity
import top.chengdongqing.wechat.core.common.util.showToast

/**
 * 在前台承接 PackageInstaller 状态并拉起系统确认页。
 * 使用 singleTop 接收同一 Session 后续的最终安装状态。
 */
class AppInstallResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStatus(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStatus(intent)
    }

    private fun handleStatus(statusIntent: Intent) {
        when (val status = statusIntent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmation =
                    statusIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmation == null) {
                    showToast("无法打开系统安装确认页面")
                    finish()
                } else {
                    startActivity(confirmation)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> finishWithMessage("应用安装成功")
            PackageInstaller.STATUS_FAILURE_ABORTED -> finishWithMessage("已取消安装")
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                finishWithMessage("系统安全策略阻止了安装")
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                finishWithMessage("应用签名或版本与已安装版本冲突")
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                finishWithMessage("此应用与当前设备不兼容")
            PackageInstaller.STATUS_FAILURE_INVALID ->
                finishWithMessage("安装包无效或不完整")
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                finishWithMessage("存储空间不足，无法安装")
            else -> {
                val detail = statusIntent.getStringExtra(
                    PackageInstaller.EXTRA_STATUS_MESSAGE
                )
                finishWithMessage(detail?.let { "应用安装失败：$it" } ?: "应用安装失败（$status）")
            }
        }
    }

    private fun finishWithMessage(message: String) {
        showToast(message)
        finish()
    }
}
