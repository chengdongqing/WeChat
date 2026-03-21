package top.chengdongqing.wechat.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * 显示提示框
 */
fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

/**
 * 复制到剪贴板
 */
fun Context.copyToClipboard(text: String, label: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

/**
 * 获取App版本号
 */
fun Context.getVersionName(): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }

    return packageInfo.versionName ?: "1.0"
}

/**
 * 跳转至系统设置
 *
 * @param isNotification 是否跳转至通知设置（若系统版本不支持，则回退至应用详情页）
 */
fun Context.navigateToAppSettings(isNotification: Boolean = false) {
    val intent = Intent().apply {
        // 处理通知设置跳转
        if (isNotification) {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        // 处理应用详情页跳转 (低版本通知设置或通用设置)
        else {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        }

        // 确保在非 Activity 环境（如 Service 或某些 Context）调用时也能正常启动
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}