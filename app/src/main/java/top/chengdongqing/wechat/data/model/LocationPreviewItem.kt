package top.chengdongqing.wechat.data.model

import android.os.Parcelable
import com.amap.api.maps.model.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationPreviewItem(
    val latLng: LatLng,
    val poiName: String = "位置",
    val address: String? = null,
    val zoom: Float = 16f
) : Parcelable