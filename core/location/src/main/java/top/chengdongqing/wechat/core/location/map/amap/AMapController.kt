package top.chengdongqing.wechat.core.location.map.amap

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import top.chengdongqing.wechat.core.location.R
import top.chengdongqing.wechat.core.location.map.MapController
import top.chengdongqing.wechat.core.location.map.MapMarkerHandle
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.util.createIconBitmap

/**
 * 基于高德地图 SDK 的 [MapController] 实现
 *
 * 所有 AMap SDK 类型均封闭在此类内部，外部通过 [MapController] 接口交互。
 */
@Stable
internal class AMapController(
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
                interval(5000)
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

    override val cameraCenter: GeoPoint?
        get() = map.cameraPosition?.target?.toGeoPoint()

    // ── 地图操作 ──────────────────────────────────────────────────────────

    override fun moveTo(point: GeoPoint, zoom: Float) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point.toLatLng(), zoom))
    }

    override fun addMarker(point: GeoPoint, icon: Bitmap?): MapMarkerHandle {
        val marker = map.addMarker(MarkerOptions().apply {
            position(point.toLatLng())
            icon?.let { icon(BitmapDescriptorFactory.fromBitmap(it)) }
        })
        return AMapMarkerHandle(marker)
    }

    override suspend fun takeSnapshot(): Bitmap? = suspendCancellableCoroutine { cont ->
        map.getMapScreenShot(object : AMap.OnMapScreenShotListener {
            private fun deliver(bitmap: Bitmap?) {
                if (cont.isActive) cont.resume(bitmap) { _, _, _ -> bitmap?.recycle() }
            }

            override fun onMapScreenShot(bitmap: Bitmap?) = deliver(bitmap)
            override fun onMapScreenShot(bitmap: Bitmap?, status: Int) = deliver(bitmap)
        })
    }

    // ── 事件监听 ──────────────────────────────────────────────────────────

    override fun setOnMapClickListener(listener: (GeoPoint) -> Unit) {
        map.setOnMapClickListener { listener(it.toGeoPoint()) }
    }

    override fun setOnPoiClickListener(listener: (GeoPoint) -> Unit) {
        map.setOnPOIClickListener { listener(it.coordinate.toGeoPoint()) }
    }

    override fun setOnTouchListener(listener: (MotionEvent) -> Unit) {
        map.setOnMapTouchListener { listener(it) }
    }

    override fun setOnLocationChangeListener(listener: (GeoPoint) -> Unit) {
        map.setOnMyLocationChangeListener { location ->
            if (location.isValid()) listener(location.toGeoPoint())
        }
    }
}

private class AMapMarkerHandle(private val marker: Marker?) : MapMarkerHandle {
    override fun remove() {
        marker?.remove()
    }
}
