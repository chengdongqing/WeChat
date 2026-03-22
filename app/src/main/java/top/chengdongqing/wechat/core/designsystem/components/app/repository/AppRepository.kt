package top.chengdongqing.wechat.core.designsystem.components.app.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.designsystem.components.app.model.AppItem
import java.io.File
import javax.inject.Inject

class AppRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * 查询已安装的应用信息
     */
    suspend fun loadInstalledApks(): List<AppItem> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val resolveInfos = packageManager.resolveInfos

        resolveInfos.mapNotNull { resolveInfo ->
            try {
                val activityInfo = resolveInfo.activityInfo
                val packageName = activityInfo.packageName

                // 获取 PackageInfo
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }

                val appInfo = packageInfo.applicationInfo
                val apkPath = appInfo?.sourceDir ?: ""
                val apkFile = File(apkPath)

                // 构造数据对象
                AppItem(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                    packageName = packageName,
                    versionName = packageInfo.versionName ?: "1.0",
                    lastModified = packageInfo.lastUpdateTime,
                    apkPath = apkPath,
                    apkSize = if (apkFile.exists()) apkFile.length() else 0L
                )
            } catch (_: PackageManager.NameNotFoundException) {
                null // 找不到包名则跳过
            }
        }.sortedByDescending {
            it.lastModified
        }
    }
}

private val PackageManager.resolveInfos: List<ResolveInfo>
    get() {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, 0)
        }
    }