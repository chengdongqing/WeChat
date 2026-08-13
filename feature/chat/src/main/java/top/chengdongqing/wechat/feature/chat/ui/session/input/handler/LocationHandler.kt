package top.chengdongqing.wechat.feature.chat.ui.session.input.handler

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.file.deleteFileByUri
import top.chengdongqing.wechat.core.location.model.LocationInfo
import top.chengdongqing.wechat.core.location.picker.rememberLocationPickerLauncher
import top.chengdongqing.wechat.core.model.MessageType

class LocationHandler(
    private val privateFileManager: PrivateFileManager,
    private val onSendMessage: (MessageContent) -> Unit
) {
    /**
     * 处理位置选择结果
     */
    suspend fun handleLocationSelection(location: LocationInfo, context: Context) {
        val localPath = location.staticMapUri?.let { uri ->
            // 拷贝到私有目录
            privateFileManager.saveMedia(
                messageType = MessageType.Location,
                sourceUri = uri
            ).also {
                // 清理临时文件
                context.deleteFileByUri(uri)
            }.getOrNull()
        }

        val content = MessageContent.Location(
            latitude = location.coordinate.latitude,
            longitude = location.coordinate.longitude,
            address = location.address ?: "",
            poiName = location.name,
            snapshotPath = localPath
        )
        onSendMessage(content)
    }
}

@Composable
fun rememberLocationHandler(
    privateFileManager: PrivateFileManager,
    onSendMessage: (MessageContent) -> Unit
): LocationHandler {
    return remember(privateFileManager, onSendMessage) {
        LocationHandler(privateFileManager, onSendMessage)
    }
}

@Composable
fun rememberLocationLauncher(
    locationHandler: LocationHandler
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val locationPicker = rememberLocationPickerLauncher { location ->
        scope.launch {
            locationHandler.handleLocationSelection(location, context)
        }
    }
    return { locationPicker.launch() }
}
