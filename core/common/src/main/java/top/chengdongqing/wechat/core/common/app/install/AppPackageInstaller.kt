package top.chengdongqing.wechat.core.common.app.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 使用 PackageInstaller.Session 安装单 APK。
 */
object AppPackageInstaller {
    const val EXTRA_FILE_PATH = "extra_file_path"

    fun launch(context: Context, file: File) {
        context.startActivity(
            Intent(context, AppInstallActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, file.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    suspend fun install(context: Context, source: File) = withContext(Dispatchers.IO) {
        require(source.isFile) { "安装文件不存在" }
        require(source.extension.equals("apk", ignoreCase = true)) {
            "不支持的安装文件"
        }

        @Suppress("DEPRECATION")
        require(context.packageManager.getPackageArchiveInfo(source.absolutePath, 0) != null) {
            "不是有效安装包"
        }
        commitSession(context, source)
    }

    private fun commitSession(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        val sessionId = installer.createSession(params)

        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().buffered().use { input ->
                    session.openWrite(apk.name, 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }

                // 使用 Activity 承接状态。若先收到 STATUS_PENDING_USER_ACTION，
                // 可从前台 Activity 合法启动系统确认页，避免后台 Receiver 被 BAL 拦截。
                val statusIntent = Intent(context, AppInstallResultActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    sessionId,
                    statusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                session.commit(pendingIntent.intentSender)
            }
        } catch (error: Throwable) {
            installer.abandonSession(sessionId)
            throw error
        }
    }
}
