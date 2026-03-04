package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.location.model.LocationInfo
import top.chengdongqing.wechat.core.designsystem.components.location.picker.rememberPickLocationLauncher
import top.chengdongqing.wechat.core.util.FileNameUtils
import top.chengdongqing.wechat.core.util.copyUriToPrivateDir
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

/**
 * 位置处理器
 *
 * 封装位置相关的所有操作
 */
class LocationHandler(
    private val onSendMessage: (MessageContent) -> Unit
) {
    /**
     * 处理位置选择结果
     */
    suspend fun handleLocationSelection(location: LocationInfo, context: Context) {
        val localPath = context.copyUriToPrivateDir(
            uri = location.staticMapUri ?: return,
            subDir = FileNameUtils.getFileConfig(MessageType.Image).dirName
        ) ?: return

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
    onSendMessage: (MessageContent) -> Unit
): LocationHandler {
    return remember(onSendMessage) {
        LocationHandler(onSendMessage)
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