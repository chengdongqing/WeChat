package top.chengdongqing.wechat.core.common.app.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

/**
 * 使用 PackageInstaller.Session 安装单 APK 或由 AppPicker 生成的 .apks。
 * 多个 APK 会写入同一个 Session，由系统原子校验并安装。
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

        val workDir = File(
            context.cacheDir,
            "app_install/${UUID.randomUUID()}"
        ).apply { mkdirs() }

        try {
            val apkFiles = if (source.extension.equals("apks", ignoreCase = true)) {
                extractApks(source, workDir)
            } else {
                require(source.extension.equals("apk", ignoreCase = true)) {
                    "不支持的安装文件"
                }
                listOf(source)
            }

            validateApks(context.packageManager, apkFiles)
            commitSession(context, apkFiles)
            // commit 前所有内容都已复制进 PackageInstaller Session。
            workDir.deleteRecursively()
        } catch (error: Throwable) {
            workDir.deleteRecursively()
            throw error
        }
    }

    private fun extractApks(archive: File, outputDir: File): List<File> {
        val output = mutableListOf<File>()
        var extractedBytes = 0L

        ZipFile(archive).use { zip ->
            val entries = zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .toList()
            require(entries.isNotEmpty() && entries.size <= MAX_APK_COUNT) {
                "APKS 文件内容无效"
            }

            entries.forEach { entry ->
                val name = entry.name
                require(
                    !name.contains('/') &&
                        !name.contains('\\') &&
                        name.endsWith(".apk", ignoreCase = true)
                ) { "APKS 包含不安全的文件" }

                val target = File(outputDir, name)
                zip.getInputStream(entry).buffered().use { input ->
                    target.outputStream().buffered().use { stream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            extractedBytes += count
                            require(extractedBytes <= MAX_EXTRACTED_BYTES) {
                                "APKS 文件过大"
                            }
                            stream.write(buffer, 0, count)
                        }
                    }
                }
                output += target
            }
        }

        require(output.any { it.name == "base.apk" }) { "APKS 缺少 base.apk" }
        return output.sortedBy { it.name != "base.apk" }
    }

    private fun validateApks(packageManager: PackageManager, apkFiles: List<File>) {
        val baseApk = apkFiles.firstOrNull { it.name == "base.apk" }
            ?: apkFiles.singleOrNull()
            ?: throw IllegalArgumentException("安装包缺少 base.apk")

        @Suppress("DEPRECATION")
        require(packageManager.getPackageArchiveInfo(baseApk.absolutePath, 0) != null) {
            "base.apk 不是有效安装包"
        }

        // PackageManager 无法把许多配置 Split 当作独立安装包解析，会返回 null。
        // 此处只校验其基本 APK 结构；包名、版本、签名及 Split 依赖关系由
        // PackageInstaller 在提交整个原子 Session 时统一执行权威校验。
        apkFiles.forEach { apk ->
            require(apk.isFile && apk.length() > 0L && hasAndroidManifest(apk)) {
                "${apk.name} 不是有效 APK"
            }
        }
    }

    private fun hasAndroidManifest(apk: File): Boolean =
        runCatching {
            ZipFile(apk).use { zip ->
                zip.getEntry("AndroidManifest.xml")?.isDirectory == false
            }
        }.getOrDefault(false)

    private fun commitSession(context: Context, apkFiles: List<File>) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        val sessionId = installer.createSession(params)

        try {
            installer.openSession(sessionId).use { session ->
                apkFiles.forEach { apk ->
                    apk.inputStream().buffered().use { input ->
                        session.openWrite(apk.name, 0, apk.length()).use { output ->
                            input.copyTo(output)
                            session.fsync(output)
                        }
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

    private const val MAX_APK_COUNT = 100
    private const val MAX_EXTRACTED_BYTES = 2L * 1024 * 1024 * 1024
}
