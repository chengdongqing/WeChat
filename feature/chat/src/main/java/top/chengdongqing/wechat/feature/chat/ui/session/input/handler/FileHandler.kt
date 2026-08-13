package top.chengdongqing.wechat.feature.chat.ui.session.input.handler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.apppicker.AppPickerRequest
import top.chengdongqing.wechat.core.apppicker.rememberAppPickerLauncher
import top.chengdongqing.wechat.core.data.handler.FileHandler
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.feature.chat.ui.file.rememberFilePickerLauncher

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

    val filePicker = rememberFilePickerLauncher { uris ->
        scope.launch {
            fileHandler.handleFileSelection(uris, context)
        }
    }

    val appPicker = rememberAppPickerLauncher { apps ->
        scope.launch {
            fileHandler.handleAppSelection(apps.toTypedArray())
        }
    }

    return remember(filePicker, appPicker) {
        FileLauncher(
            pickFile = { filePicker.launch() },
            pickApk = { appPicker.launch(AppPickerRequest(maxSelection = 99)) }
        )
    }
}

data class FileLauncher(
    val pickFile: () -> Unit,
    val pickApk: () -> Unit
)
