package top.chengdongqing.wechat.core.location.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 传递给位置预览页的配置
 */
@Parcelize
data class LocationPreviewInfo(
    val coordinate: GeoPoint,
    val name: String = "位置",
    val address: String? = null,
    val zoomLevel: Float = 16f
) : Parcelable
