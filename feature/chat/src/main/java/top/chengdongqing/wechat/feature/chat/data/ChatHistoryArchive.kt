package top.chengdongqing.wechat.feature.chat.data

import top.chengdongqing.wechat.core.data.model.ChatHistoryItem
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val ASSET_PREFIX = "assets/"

/** 将记录中的本地附件收集进单个归档，并把绝对路径改成归档内相对路径。 */
fun createChatHistoryArchive(
    items: List<ChatHistoryItem>,
    archiveFile: File
): List<ChatHistoryItem> {
    archiveFile.parentFile?.mkdirs()
    val archivedPaths = mutableMapOf<String, String>()
    var index = 0
    ZipOutputStream(archiveFile.outputStream().buffered()).use { zip ->
        fun archive(item: ChatHistoryItem): ChatHistoryItem {
            fun archivePath(path: String?): String? {
                val file = path?.let(::File)?.takeIf { it.isFile } ?: return null
                return archivedPaths.getOrPut(file.absolutePath) {
                    // 归档名只取稳定序号和扩展名；不能包含解包后的旧条目名，
                    // 否则再次转发会从 0_x.jpg 变成 0_0_x.jpg，破坏 checksum 去重。
                    val extension = file.extension
                        .replace(Regex("[^A-Za-z0-9]"), "")
                        .takeIf(String::isNotBlank)
                    val entryName = "$ASSET_PREFIX${index++}${extension?.let { ".$it" }.orEmpty()}"
                    // 固定时间戳，使相同附件集合生成完全相同的归档和 checksum。
                    zip.putNextEntry(ZipEntry(entryName).apply { time = 0L })
                    file.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                    entryName
                }
            }

            val relativePath = archivePath(item.localPath)
            val nested = item.nestedHistory
            val music = item.music
            return item.copy(
                localPath = relativePath,
                nestedHistory = nested?.copy(
                    items = nested.items.map(::archive)
                ),
                music = music?.copy(
                    audioPath = archivePath(music.audioPath),
                    coverPath = archivePath(music.coverPath)
                )
            )
        }
        return items.map(::archive)
    }
}

/** 解包归档并将相对附件路径恢复为接收设备上的绝对路径。 */
fun resolveChatHistoryAssets(
    items: List<ChatHistoryItem>,
    archivePath: String?
): List<ChatHistoryItem> {
    val archive = archivePath?.let(::File)?.takeIf { it.isFile } ?: return items
    val outputDir = File(archive.parentFile, "${archive.nameWithoutExtension}_assets")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val target = File(outputDir, entry.name).canonicalFile
                check(target.path.startsWith(outputDir.canonicalPath + File.separator)) {
                    "非法聊天记录附件路径"
                }
                if (entry.isDirectory) target.mkdirs() else {
                    target.parentFile?.mkdirs()
                    target.outputStream().buffered().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    fun resolve(item: ChatHistoryItem): ChatHistoryItem {
        fun resolvePath(path: String?): String? = path
            ?.takeIf { it.startsWith(ASSET_PREFIX) }
            ?.let { File(outputDir, it).takeIf(File::isFile)?.absolutePath }
            ?: path?.takeIf { File(it).isFile }

        val resolvedPath = resolvePath(item.localPath)
        val nested = item.nestedHistory
        val music = item.music
        return item.copy(
            localPath = resolvedPath,
            nestedHistory = nested?.copy(
                items = nested.items.map(::resolve)
            ),
            music = music?.copy(
                audioPath = resolvePath(music.audioPath),
                coverPath = resolvePath(music.coverPath)
            )
        )
    }
    return items.map(::resolve)
}
