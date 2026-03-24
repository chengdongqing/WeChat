package top.chengdongqing.wechat.core.location.map

/**
 * Marker 不透明句柄
 *
 * 调用方通过此接口移除 Marker，无需感知底层 SDK 的 Marker 类型。
 */
interface MapMarkerHandle {
    fun remove()
}
