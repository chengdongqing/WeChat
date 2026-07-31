package top.chengdongqing.wechat.core.location.map.amap

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Stable
import com.amap.api.maps.AMap
import com.amap.api.maps.AMapOptions
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.location.map.MapController
import top.chengdongqing.wechat.core.location.map.MapMarkerHandle
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.util.createIconBitmap

/**
 * 基于高德地图 SDK 的 [MapController] 实现
 */
@Stable
class AMapController(
    context: Context,
    isDarkTheme: Boolean,
    private val scope: CoroutineScope
) : MapController {

    private val mapView: MapView
    private val map: AMap

    init {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)

        val options = AMapOptions().apply {
            logoPosition(AMapOptions.LOGO_POSITION_BOTTOM_RIGHT)
            zoomControlsEnabled(false)
            mapType(if (isDarkTheme) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL)
        }
        mapView = MapView(context, options)
        map = mapView.map
    }

    // ── 渲染 ──────────────────────────────────────────────────────────────

    override val view: View get() = mapView

    // ── 生命周期 ──────────────────────────────────────────────────────────

    override fun onCreate(savedState: Bundle?) {
        mapView.onCreate(savedState)
    }

    override fun onResume() {
        mapView.onResume()
        mapView.postInvalidate()
    }

    override fun onPause(outState: Bundle) {
        mapView.onSaveInstanceState(outState)
        mapView.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        mapView.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) mapView.onLowMemory()
    }

    // ── 定位 ──────────────────────────────────────────────────────────────

    override fun enableMyLocation(context: Context) {
        scope.launch {
            map.myLocationStyle = MyLocationStyle().apply {
                // 选择器生命周期很短，缩短重试间隔可以更快拿到首次有效结果。
                interval(1000)
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                val icon = createIconBitmap(context, R.drawable.ic_location_rotatable, 90, 90, -60f)
                    ?.let { BitmapDescriptorFactory.fromBitmap(it) }
                myLocationIcon(icon)
                radiusFillColor(Color.TRANSPARENT)
                strokeWidth(0f)
            }
            map.isMyLocationEnabled = true
        }
    }

    override fun disableMyLocation() {
        map.isMyLocationEnabled = false
    }

    // ── 地图状态 ──────────────────────────────────────────────────────────

    override val currentLocation: GeoPoint?
        get() = map.myLocation?.takeIf { it.isValid() }?.toGeoPoint()

    override val currentBearing: Float?
        get() = map.myLocation?.takeIf { it.isValid() && it.hasBearing() }?.bearing

    override val cameraCenter: GeoPoint?
        get() = map.cameraPosition?.target?.toGeoPoint()

    // ── 地图操作 ──────────────────────────────────────────────────────────

    override fun moveTo(point: GeoPoint, zoom: Float) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point.toLatLng(), zoom))
    }

    override fun addMarker(point: GeoPoint, icon: Bitmap?, bearing: Float?): MapMarkerHandle {
        val marker = map.addMarker(MarkerOptions().apply {
            position(point.toLatLng())
            icon?.let {
                icon(BitmapDescriptorFactory.fromBitmap(it))
            }
            bearing?.let {
                anchor(.5f, .5f)
                rotateAngle(-it)
            }
        })
        return AMapMarkerHandle(marker)
    }

    override suspend fun takeSnapshot(
        markerPoint: GeoPoint?,
        markerIcon: Bitmap?
    ): Bitmap? {
        // 定位蓝点属于独立的 GL 图层。先关闭并留出一帧刷新时间，避免它偶尔混入缩略图。
        map.isMyLocationEnabled = false
        delay(SNAPSHOT_RENDER_DELAY_MS)

        return suspendCancellableCoroutine { cont ->
            map.getMapScreenShot(object : AMap.OnMapScreenShotListener {
                private fun deliver(bitmap: Bitmap?) {
                    val result = bitmap?.withMarker(markerPoint, markerIcon)
                    if (cont.isActive) {
                        cont.resume(result) { _, _, _ ->
                            if (result !== bitmap) result?.recycle()
                            bitmap?.recycle()
                        }
                        if (result !== bitmap) bitmap?.recycle()
                    } else {
                        if (result !== bitmap) result?.recycle()
                        bitmap?.recycle()
                    }
                }

                override fun onMapScreenShot(bitmap: Bitmap?) = deliver(bitmap)
                override fun onMapScreenShot(bitmap: Bitmap?, status: Int) = deliver(bitmap)
            })
        }
    }

    private fun Bitmap.withMarker(point: GeoPoint?, icon: Bitmap?): Bitmap {
        if (point == null || icon == null) return this

        val result = copy(Bitmap.Config.ARGB_8888, true)
        val screenPoint = map.projection.toScreenLocation(point.toLatLng())
        Canvas(result).drawBitmap(
            icon,
            screenPoint.x - icon.width / 2f,
            screenPoint.y - icon.height.toFloat(),
            null
        )
        return result
    }

    private companion object {
        const val SNAPSHOT_RENDER_DELAY_MS = 100L
    }

    // ── 事件监听 ──────────────────────────────────────────────────────────

    override fun setOnMapClickListener(listener: (GeoPoint) -> Unit) {
        map.setOnMapClickListener {
            listener(it.toGeoPoint())
        }
    }

    override fun setOnPoiClickListener(listener: (GeoPoint) -> Unit) {
        map.setOnPOIClickListener {
            listener(it.coordinate.toGeoPoint())
        }
    }

    override fun setOnTouchListener(listener: (MotionEvent) -> Unit) {
        map.setOnMapTouchListener {
            listener(it)
        }
    }

    override fun setOnLocationChangeListener(listener: (GeoPoint, Float?) -> Unit) {
        map.setOnMyLocationChangeListener { location ->
            if (location.isValid()) {
                listener(
                    location.toGeoPoint(),
                    location.bearing.takeIf { location.hasBearing() }
                )
            }
        }
    }
}

private class AMapMarkerHandle(private val marker: Marker?) : MapMarkerHandle {
    override fun update(point: GeoPoint, bearing: Float?) {
        marker?.position = point.toLatLng()
        // 高德 Marker 的旋转方向与 Android Location bearing 相反。
        bearing?.let { marker?.rotateAngle = -it }
    }

    override fun remove() {
        marker?.remove()
    }
}
