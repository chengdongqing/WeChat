package top.chengdongqing.wechat.core.designsystem.components.location.model

import android.net.Uri
import android.os.Parcelable
import com.amap.api.maps.model.LatLng
import kotlinx.parcelize.Parcelize

/**
 * 位置详细信息实体
 * 用于位置选择和搜索等
 */
@Parcelize
data class LocationInfo(
    val id: String? = null,
    val name: String,
    val address: String? = null,
    val distanceMetres: Int? = null,
    val coordinate: LatLng,
    val staticMapUrl: Uri? = null
) : Parcelable