package top.chengdongqing.wechat.core.location.model

import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import top.chengdongqing.wechat.core.location.R
import java.net.URLEncoder

/**
 * 第三方地图服务商类型及其协议适配
 */
enum class MapType(
    @get:StringRes val labelRes: Int,
    val packageName: String
) {
    AMap(R.string.map_amap, "com.autonavi.minimap") {
        override fun buildUri(lat: Double, lng: Double, name: String): Uri {
            return "amapuri://route/plan?dlat=$lat&dlon=$lng&dname=$name&t=0".toUri()
        }
    },
    Baidu(R.string.map_baidu, "com.baidu.BaiduMap") {
        override fun buildUri(lat: Double, lng: Double, name: String): Uri {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            return "baidumap://map/direction?destination=latlng:$lat,$lng|name:$encodedName&coord_type=gcj02&mode=driving".toUri()
        }
    },
    Tencent(R.string.map_tencent, "com.tencent.map") {
        override fun buildUri(lat: Double, lng: Double, name: String): Uri {
            return "qqmap://map/routeplan?to=$name&tocoord=$lat,$lng&type=drive".toUri()
        }
    },
    Google(R.string.map_google, "com.google.android.apps.maps") {
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