package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.app.model.AppResult
import top.chengdongqing.wechat.core.designsystem.components.app.rememberPickAppLauncher
import top.chengdongqing.wechat.core.util.copyUriToPrivateDir
import top.chengdongqing.wechat.core.util.getFileConfig
import top.chengdongqing.wechat.core.util.getFileMetadata
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

/**
 * 文件处理器
 */
class FileHandler(
    private val onSendMessage: (MessageContent) -> Unit
) {
    /**
     * 处理文件选择结果
     */
    suspend fun handleFileSelection(uris: List<Uri>, context: Context) {
        uris.forEachIndexed { index, uri ->
            // 解析元数据
            val metadata = context.getFileMetadata(uri) ?: return

            // 拷贝到私有目录
            val localPath = context.copyUriToPrivateDir(
                uri = uri,
                subDir = MessageType.File.getFileConfig().dirName
            ) ?: return

            // 构建消息内容
            val content = MessageContent.File(
                localPath = localPath,
                mimeType = metadata.mimeType,
                filename = metadata.filename,
                size = metadata.size
            )

            // 发送
            onSendMessage(content)

            if (index < uris.lastIndex) delay(50)
        }
    }

    /**
     * 处理 App 选择结果
     */
    suspend fun handleAppSelection(apps: Array<AppResult>) {
        apps.forEachIndexed { index, app ->
            // 构建消息内容
            val content = MessageContent.File(
                localPath = app.filePath,
                mimeType = "application/vnd.android.package-archive",
                filename = app.fileName,
                size = app.fileSize
            )

            // 发送
            onSendMessage(content)

            if (index < apps.lastIndex) delay(50)
        }
    }
}

@Composable
fun rememberFileHandler(onSendMessage: (MessageContent) -> Unit): FileHandler {
    return remember(onSendMessage) {
        FileHandler(onSendMessage)
    }
}

@Composable
fun rememberFileLauncher(
    fileHandler: FileHandler
): FileLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        scope.launch {
            fileHandler.handleFileSelection(uris, context)
        }
    }

    val pickApk = rememberPickAppLauncher { apps ->
        scope.launch {
            fileHandler.handleAppSelection(apps)
        }
    }

    return remember(pickFileLauncher) {
        FileLauncher(
            pickFile = { pickFileLauncher.launch("*/*") },
            pickApk = { pickApk(9) }
        )
    }
}

data class FileLauncher(
    val pickFile: () -> Unit,
    val pickApk: () -> Unit
)