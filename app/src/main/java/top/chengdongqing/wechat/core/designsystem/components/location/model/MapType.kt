package top.chengdongqing.wechat.core.designsystem.components.location.model

import android.net.Uri
import androidx.core.net.toUri
import java.net.URLEncoder

/**
 * 第三方地图服务商类型及其协议适配
 */
enum class MapType(val appName: String, val packageName: String) {
    AMap("高德", "com.autonavi.minimap") {
        override fun buildUri(lat: Double, lng: Double, name: String): Uri {
            return "amapuri://route/plan?dlat=$lat&dlon=$lng&dname=$name&t=0".toUri()
        }
    },
    Baidu("百度", "com.baidu.BaiduMap") {
        override fun buildUri(lat: Double, lng: Double, name: String): Uri {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            return "baidumap://map/direction?destination=latlng:$lat,$lng|name:$encodedName&coord_type=gcj02&mode=driving".toUri()
        }
    },
    Tencent("腾讯", "com.tencent.map") {
        override fun buildUri(lat: Double, lng: Double, name: String): Uri {
            return "qqmap://map/routeplan?to=$name&tocoord=$lat,$lng&type=drive".toUri()
        }
    },
    Google("谷歌", "com.google.android.apps.maps") {
        override fun buildUri(lat: Double, lng: Double, name: String): Uri {
            return "google.navigation:q=$lat,$lng".toUri()
        }
    };

    abstract fun buildUri(lat: Double, lng: Double, name: String): Uri

    companion object {
        fun ofIndex(index: Int): MapType? {
            return entries.getOrNull(index)
        }
    }
}