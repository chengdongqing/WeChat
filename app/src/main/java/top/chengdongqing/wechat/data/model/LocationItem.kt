package top.chengdongqing.wechat.data.model

import android.net.Uri
import android.os.Parcelable
import com.amap.api.maps.model.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationItem(
    val id: String? = null,
    val poiName: String,
    val address: String? = null,
    val distance: Int? = null,
    val latLng: LatLng,
    val snapshotUri: Uri? = null
) : Parcelable