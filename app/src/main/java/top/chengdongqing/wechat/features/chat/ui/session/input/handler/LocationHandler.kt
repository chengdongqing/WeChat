package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.location.model.LocationInfo
import top.chengdongqing.wechat.core.location.picker.rememberPickLocationLauncher
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.util.deleteFileByUri
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

class LocationHandler(
    private val privateFileManager: PrivateFileManager,
    private val onSendMessage: (MessageContent) -> Unit
) {
    /**
     * 处理位置选择结果
     */
    suspend fun handleLocationSelection(location: LocationInfo, context: Context) {
        val uri = location.staticMapUri ?: return

        // 拷贝到私有目录
        val localPath = privateFileManager.saveMedia(
            messageType = MessageType.Location,
            sourceUri = uri
        ).getOrThrow()

        // 清理临时文件
        context.deleteFileByUri(uri)

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
    return remember(onSendMessage) {
        LocationHandler(privateFileManager, onSendMessage)
    }
}

@Composable
fun rememberLocationLauncher(
    locationHandler: LocationHandler
): LocationLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickLocation = rememberPickLocationLauncher { location ->
        scope.launch {
            locationHandler.handleLocationSelection(location, context)
        }
    }

    return remember(pickLocation) {
        LocationLauncher(pickLocation)
    }
}

/**
 * 位置启动器集合
 */
data class LocationLauncher(
    val pickLocation: () -> Unit
)