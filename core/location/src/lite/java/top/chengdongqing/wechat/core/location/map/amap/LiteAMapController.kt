package top.chengdongqing.wechat.core.location.map.amap

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Stable
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.AMap
import com.amap.api.maps.AMapOptions
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.services.core.ServiceSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.location.map.MapController
import top.chengdongqing.wechat.core.location.map.MapMarkerHandle
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.util.createIconBitmap

/**
 * 基于高德地图轻量版 SDK（Lite3DMap）的 [MapController] 实现
 *
 * 与完整版 3D SDK 的主要差异：
 * - [AMap] 对象通过 [MapView.getMapAsyn] 异步获取，用 [mapDeferred] 持有
 * - 不支持截图等
 */
@Stable
class LiteAMapController(
    private val context: Context,
    isDarkTheme: Boolean,
    private val scope: CoroutineScope
) : MapController, LocationSource {

    private val mapView: MapView

    /** 地图就绪后完成；在此之前所有需要 [AMap] 的操作均会挂起等待 */
    private val mapDeferred = CompletableDeferred<AMap>()

    /** 地图就绪后的快捷访问，就绪前返回 null（仅用于无法挂起的属性读取） */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val map: AMap?
        get() = if (mapDeferred.isCompleted) mapDeferred.getCompleted() else null

    private var locationChangeListener: LocationSource.OnLocationChangedListener? = null
    private var locationClient: AMapLocationClient? = null
    private var lastLocation: GeoPoint? = null

    init {
        runCatching {
            MapsInitializer.initialize(context)

            AMapLocationClient.updatePrivacyShow(context, true, true)
            AMapLocationClient.updatePrivacyAgree(context, true)
            ServiceSettings.updatePrivacyShow(context, true, true)
            ServiceSettings.updatePrivacyAgree(context, true)
        }

        val options = AMapOptions().apply {
            mapType(if (isDarkTheme) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL)
            rotateGesturesEnabled(false)
        }
        mapView = MapView(context, options)
    }

    // ── 渲染 ──────────────────────────────────────────────────────────────

    override val view: View get() = mapView

    // ── 生命周期 ──────────────────────────────────────────────────────────

    override fun onCreate(savedState: Bundle?) {
        mapView.onCreate(savedState)
        // 轻量版 SDK 无同步 map 属性，必须通过异步回调获取 AMap 实例
        mapView.getMapAsyn { amap ->
            amap.uiSettings.isZoomGesturesEnabled = true
            mapDeferred.complete(amap)
        }
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
            val map = mapDeferred.await()
            map.setLocationSource(this@LiteAMapController)

            map.myLocationStyle = MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                val icon = createIconBitmap(context, R.drawable.ic_location_rotatable, 90, 90, -60f)
                    ?.let { BitmapDescriptorFactory.fromBitmap(it) }
                myLocationIcon(icon)
                radiusFillColor(Color.TRANSPARENT)
                strokeWidth(0f)
            }
            map.setMyLocationEnabled(true)
        }
    }

    override fun activate(listener: LocationSource.OnLocationChangedListener?) {
        locationChangeListener = listener
        if (locationClient == null) {
            locationClient = AMapLocationClient(context).apply {
                setLocationListener { location ->
                    if (location != null && location.errorCode == 0) {
                        lastLocation = location.toGeoPoint()
                        // 将定位结果传给地图
                        locationChangeListener?.onLocationChanged(location)
                    }
                }
                startLocation() // 启动定位
            }
        }
    }

    override fun deactivate() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
        locationChangeListener = null
    }

    override fun disableMyLocation() {
        scope.launch {
            mapDeferred.await().setMyLocationEnabled(false)
        }
    }

    // ── 地图状态 ──────────────────────────────────────────────────────────

    override val currentLocation: GeoPoint?
        get() = lastLocation

    override val cameraCenter: GeoPoint?
        get() = map?.cameraPosition?.target?.toGeoPoint()

    // ── 地图操作 ──────────────────────────────────────────────────────────

    override fun moveTo(point: GeoPoint, zoom: Float) {
        scope.launch {
            mapDeferred.await()
                .animateCamera(CameraUpdateFactory.newLatLngZoom(point.toLatLng(), zoom))
        }
    }

    override fun addMarker(point: GeoPoint, icon: Bitmap?): MapMarkerHandle {
        // addMarker 需同步返回句柄，实际添加操作在地图就绪后异步执行
        val handle = DeferredAMapMarkerHandle()
        scope.launch {
            val marker = mapDeferred.await().addMarker(MarkerOptions().apply {
                position(point.toLatLng())
                icon?.let {
                    icon(BitmapDescriptorFactory.fromBitmap(it))
                }
            })
            handle.setMarker(marker)
        }
        return handle
    }

    /** 轻量版 SDK 不支持截图，始终返回 null */
    override suspend fun takeSnapshot(): Bitmap? = null

    // ── 事件监听 ──────────────────────────────────────────────────────────

    override fun setOnMapClickListener(listener: (GeoPoint) -> Unit) {
        scope.launch {
            mapDeferred.await().setOnMapClickListener {
                listener(it.toGeoPoint())
            }
        }
    }

    /** 轻量版 SDK 不支持 POI 点击事件 */
    override fun setOnPoiClickListener(listener: (GeoPoint) -> Unit) = Unit

    override fun setOnTouchListener(listener: (MotionEvent) -> Unit) {
        scope.launch {
            mapDeferred.await().setOnMapTouchListener {
                listener(it)
            }
        }
    }

    override fun setOnLocationChangeListener(listener: (GeoPoint) -> Unit) {
        scope.launch {
            mapDeferred.await().setOnMyLocationChangeListener { location ->
                if (location.isValid()) {
                    listener(location.toGeoPoint())
                }
            }
        }
    }
}

/**
 * 异步 Marker 句柄：addMarker 同步返回此对象，Marker 实例在地图就绪后填入。
 * 调用 [remove] 时若 Marker 尚未就绪则静默忽略（实际场景中极少发生）。
 */
private class DeferredAMapMarkerHandle : MapMarkerHandle {
    @Volatile
    private var marker: Marker? = null
    fun setMarker(m: Marker?) {
        marker = m
    }

    override fun remove() {
        marker?.remove()
    }
}
