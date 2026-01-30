package top.chengdongqing.wechat.ui.chat.session.input.handler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.chengdongqing.wechat.data.model.LocationItem
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.components.location.picker.rememberPickLocationLauncher

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
    fun handleLocationSelection(location: LocationItem) {
        val content = MessageContent.Location(
            latitude = location.latLng.latitude,
            longitude = location.latLng.longitude,
            address = location.address ?: "",
            poiName = location.poiName,
            snapshotUri = location.snapshotUri
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