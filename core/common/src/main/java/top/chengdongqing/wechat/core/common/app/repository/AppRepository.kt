package top.chengdongqing.wechat.core.common.app.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.app.model.AppItem
import top.chengdongqing.wechat.core.common.app.model.AppResult
import java.io.File
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * 查询已安装的应用信息
     */
    suspend fun loadInstalledApks(): List<AppItem> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val resolveInfos = packageManager.resolveInfos

        resolveInfos.distinctBy { it.activityInfo?.packageName }.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName
            val appInfo = activityInfo.applicationInfo
            val apkPath = appInfo.sourceDir?.takeIf { File(it).isFile }
                ?: return@mapNotNull null
            val baseApk = File(apkPath)

            AppItem(
                name = resolveInfo.loadLabel(packageManager).toString(),
                packageName = packageName,
                // 列表不展示版本号；延迟到用户确认选择时再查询。
                versionName = "",
                lastModified = baseApk.lastModified(),
                apkPath = apkPath,
                splitApkPaths = appInfo.splitSourceDirs
                    ?.filter { File(it).isFile }
                    .orEmpty()
            )
        }.sortedByDescending {
            it.lastModified
        }
    }

    /**
     * 仅在对应 LazyColumn 项进入组合时调用，避免首次打开就解码全部应用图标。
     */
    suspend fun loadIcon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        iconCache.get(packageName) ?: runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()?.also { iconCache.put(packageName, it) }
    }

    /**
     * 普通 APK 直接分享；Split APK 打包为 .apks，保留 base 与全部 split，
     * 避免只发送 base.apk 后在接收设备上无法安装。
     */
    suspend fun prepareForSharing(apps: List<AppItem>): List<AppResult> =
        withContext(Dispatchers.IO) {
            apps.map { app ->
                val versionName = context.packageManager.versionNameOf(app.packageName)
                if (app.splitApkPaths.isEmpty()) {
                    val apk = File(app.apkPath)
                    AppResult(
                        fileName = "${app.safeFileName}-v$versionName.apk",
                        filePath = apk.absolutePath,
                        fileSize = apk.length(),
                        mimeType = APK_MIME_TYPE
                    )
                } else {
                    createSplitArchive(app, versionName)
                }
            }
        }

    private fun createSplitArchive(app: AppItem, versionName: String): AppResult {
        val outputDir = File(context.cacheDir, "shared_apps").apply { mkdirs() }
        val archive = File(
            outputDir,
            "${app.safeFileName}-${app.packageName}-v$versionName.apks"
        )
        val sources = listOf(File(app.apkPath)) + app.splitApkPaths.map(::File)

        runCatching {
            ZipOutputStream(archive.outputStream().buffered()).use { zip ->
                // APK 本身已经压缩，禁用二次压缩可显著减少点击“完成”后的等待。
                zip.setLevel(Deflater.NO_COMPRESSION)
                sources.forEachIndexed { index, source ->
                    val entryName = if (index == 0) "base.apk" else source.name
                    zip.putNextEntry(ZipEntry(entryName))
                    source.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }.onFailure {
            archive.delete()
        }.getOrThrow()

        return AppResult(
            fileName = "${app.safeFileName}-v$versionName.apks",
            filePath = archive.absolutePath,
            fileSize = archive.length(),
            mimeType = APKS_MIME_TYPE
        )
    }

    private val AppItem.safeFileName: String
        get() = name.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { packageName }

    private val iconCache = LruCache<String, Drawable>(ICON_CACHE_ENTRIES)

    private companion object {
        const val ICON_CACHE_ENTRIES = 80
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val APKS_MIME_TYPE = "application/zip"
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

private fun PackageManager.versionNameOf(packageName: String): String =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, 0)
        }.versionName ?: "1.0"
    }.getOrDefault("1.0")
