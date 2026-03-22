package top.chengdongqing.wechat.core.location.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 位置详细信息实体，用于位置选择和消息发送
 */
@Parcelize
data class LocationInfo(
    val id: String? = null,
    val name: String,
    val address: String? = null,
    val distanceMetres: Int? = null,
    val coordinate: GeoPoint,
    val staticMapUri: Uri? = null
) : Parcelable
