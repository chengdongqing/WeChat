package top.chengdongqing.wechat.core.location.map

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Stable
import top.chengdongqing.wechat.core.location.model.GeoPoint

/**
 * 地图控制器抽象接口
 *
 * 封装地图 SDK 的核心操作，使业务逻辑与具体 SDK 完全解耦。
 * 生命周期方法由 WeMap 组件统一调用，业务层无需关心。
 */
@Stable
interface MapController {

    // ── 渲染 ──────────────────────────────────────────────────────────────

    /** 供 AndroidView 渲染的原生 View，由具体 SDK 实现提供 */
    val view: View

    // ── 生命周期 ──────────────────────────────────────────────────────────

    fun onCreate(savedState: Bundle?)
    fun onResume()
    fun onPause(outState: Bundle)
    fun onDestroy()
    fun onLowMemory()
    fun onTrimMemory(level: Int)

    // ── 定位 ──────────────────────────────────────────────────────────────

    /** 启用定位蓝点，需在权限授予后调用 */
    fun enableMyLocation(context: Context)

    /** 关闭定位蓝点 */
    fun disableMyLocation()

    // ── 地图状态 ──────────────────────────────────────────────────────────

    /** 当前设备定位坐标，未定位时为 null */
    val currentLocation: GeoPoint?

    /** 当前地图视野中心坐标 */
    val cameraCenter: GeoPoint?

    // ── 地图操作 ──────────────────────────────────────────────────────────

    /**
     * 将地图视野移动到指定坐标
     *
     * @param point 目标坐标
     * @param zoom 缩放级别，默认 16
     */
    fun moveTo(point: GeoPoint, zoom: Float = 16f)

    /**
     * 在指定坐标添加 Marker
     *
     * @param point 坐标
     * @param icon 自定义图标 Bitmap，null 时使用默认样式
     * @return 句柄，可调用 [MapMarkerHandle.remove] 移除
     */
    fun addMarker(point: GeoPoint, icon: Bitmap? = null): MapMarkerHandle

    /**
     * 截取当前地图快照。
     *
     * 选点图标由截图实现直接合成，避免地图 SDK 的异步 Marker 图层尚未绘制完成。
     */
    suspend fun takeSnapshot(markerPoint: GeoPoint? = null, markerIcon: Bitmap? = null): Bitmap?

    // ── 事件监听 ──────────────────────────────────────────────────────────

    fun setOnMapClickListener(listener: (GeoPoint) -> Unit)
    fun setOnPoiClickListener(listener: (GeoPoint) -> Unit)
    fun setOnTouchListener(listener: (MotionEvent) -> Unit)
    fun setOnLocationChangeListener(listener: (GeoPoint) -> Unit)
}
