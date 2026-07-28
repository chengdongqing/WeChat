package top.chengdongqing.wechat.core.common.permission

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

/** 来电展示特殊权限的检测与系统设置入口。 */
object CallNotificationPermissionManager {
    fun canUseFullScreenIntent(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun canDisplayOverOtherApps(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun needsMiuiCallPermissions(): Boolean =
        Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    fun openFullScreenIntentSettings(context: Context) {
        open(context, fullScreenIntentSettingsIntent(context))
    }

    fun openOverlaySettings(context: Context) {
        open(context, overlaySettingsIntent(context))
    }

    /**
     * MIUI/HyperOS additionally guards "锁屏显示" and "后台弹出界面" with private
     * permissions (MIUIOP 10020/10021). There is no supported API to grant or
     * reliably query them, so take the user to the app's MIUI permission editor.
     */
    fun openMiuiPermissionSettings(context: Context) {
        open(context, miuiPermissionSettingsIntent(context))
    }

    fun fullScreenIntentSettingsIntent(context: Context): Intent =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
        } else {
            appDetailsIntent(context)
        }).apply {
            data = "package:${context.packageName}".toUri()
        }

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )

    fun miuiPermissionSettingsIntent(context: Context): Intent {
        val candidates = listOf(
            "com.miui.permcenter.permissions.PermissionsEditorActivity",
            "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
        )
        return candidates
            .asSequence()
            .map { className ->
                Intent("miui.intent.action.APP_PERM_EDITOR")
                    .setComponent(ComponentName("com.miui.securitycenter", className))
                    .putExtra("extra_pkgname", context.packageName)
            }
            .firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?: appDetailsIntent(context)
    }

    private fun open(context: Context, intent: Intent) {
        intent.data = "package:${context.packageName}".toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { context.startActivity(appDetailsIntent(context)) }
    }

    private fun appDetailsIntent(context: Context) =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
