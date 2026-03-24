package top.chengdongqing.wechat.core.location.map.amap

import android.location.Location
import android.os.Build
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import top.chengdongqing.wechat.core.location.model.GeoPoint

// GeoPoint ↔ AMap 类型转换

internal fun GeoPoint.toLatLng() = LatLng(latitude, longitude)
internal fun GeoPoint.toLatLonPoint() = LatLonPoint(latitude, longitude)
internal fun LatLng.toGeoPoint() = GeoPoint(latitude, longitude)
internal fun LatLonPoint.toGeoPoint() = GeoPoint(latitude, longitude)
internal fun Location.toGeoPoint() = GeoPoint(latitude, longitude)

/** 判断定位结果是否有效（已完成定位，坐标非默认值） */
internal fun Location.isValid() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    isComplete
} else {
    latitude != 0.0 && longitude != 0.0
}
