package top.chengdongqing.wechat.core.location.map

import top.chengdongqing.wechat.core.location.model.GeoPoint

/**
 * Marker 不透明句柄
 *
 * 调用方通过此接口移除 Marker，无需感知底层 SDK 的 Marker 类型。
 */
interface MapMarkerHandle {
    /** 原地更新标记，避免持续定位时反复删除/创建造成闪烁。 */
    fun update(point: GeoPoint, bearing: Float? = null)
    fun remove()
}
