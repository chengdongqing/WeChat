package top.chengdongqing.wechat.feature.chat.ui.session.input.handler

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.app.rememberPickAppLauncher
import top.chengdongqing.wechat.core.common.file.PrivateFileManager
import top.chengdongqing.wechat.core.data.handler.FileHandler
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.feature.chat.ui.file.FileSelectionActivity

@Composable
fun rememberFileHandler(
    privateFileManager: PrivateFileManager,
    onSendMessage: (MessageContent) -> Unit,
): FileHandler {
    return remember(privateFileManager, onSendMessage) {
        FileHandler(privateFileManager, onSendMessage)
    }
}

@Composable
fun rememberFileLauncher(
    fileHandler: FileHandler
): FileLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = FileSelectionActivity.readResult(result.data)
            scope.launch {
                fileHandler.handleFileSelection(uris, context)
            }
        }
    }

    val pickApk = rememberPickAppLauncher { apps ->
        scope.launch {
            fileHandler.handleAppSelection(apps)
        }
    }

    return remember(pickFileLauncher) {
        FileLauncher(
            pickFile = { FileSelectionActivity.launch(context, pickFileLauncher) },
            pickApk = { pickApk(99) }
        )
    }
}

data class FileLauncher(
    val pickFile: () -> Unit,
    val pickApk: () -> Unit
)
