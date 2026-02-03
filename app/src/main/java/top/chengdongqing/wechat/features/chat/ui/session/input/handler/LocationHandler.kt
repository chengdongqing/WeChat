package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.chengdongqing.wechat.core.designsystem.components.location.model.LocationInfo
import top.chengdongqing.wechat.core.designsystem.components.location.picker.rememberPickLocationLauncher
import top.chengdongqing.wechat.data.model.MessageContent

/**
 * 位置处理器
 *
 * 封装位置相关的所有操作
 */
class LocationHandler(
    private val onSendMessage: (MessageContent, (() -> Unit)?) -> Unit
) {
    /**
     * 处理位置选择结果
     */
    fun handleLocationSelection(location: LocationInfo) {
        val content = MessageContent.Location(
            latitude = location.coordinate.latitude,
            longitude = location.coordinate.longitude,
            address = location.address ?: "",
            poiName = location.name,
            snapshotUri = location.staticMapUrl
        )
        onSendMessage(content, null)
    }
}

@Composable
fun rememberLocationHandler(
    onSendMessage: (MessageContent, (() -> Unit)?) -> Unit
): LocationHandler {
    return remember(onSendMessage) {
        LocationHandler(onSendMessage)
    }
}

@Composable
fun rememberLocationLauncher(
    locationHandler: LocationHandler
): LocationLauncher {
    val pickLocation = rememberPickLocationLauncher { location ->
        locationHandler.handleLocationSelection(location)
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