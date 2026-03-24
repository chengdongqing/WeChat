package top.chengdongqing.wechat.core.location.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * SDK 无关的地理坐标
 */
@Parcelize
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) : Parcelable
