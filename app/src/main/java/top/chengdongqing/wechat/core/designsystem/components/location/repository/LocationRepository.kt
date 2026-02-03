package top.chengdongqing.wechat.core.designsystem.components.location.repository

import com.amap.api.maps.model.LatLng
import com.amap.api.services.geocoder.RegeocodeAddress
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.designsystem.components.location.model.LocationInfo

interface LocationRepository {
    suspend fun locationToAddress(latLng: LatLng): RegeocodeAddress?

    suspend fun search(
        location: LatLng?,
        keyword: String,
        pageNum: Int,
        pageSize: Int,
        current: LatLng?
    ): List<LocationInfo>

    val currentLocation: Flow<LatLng?>

    suspend fun saveCurrentLocation(latLng: LatLng)
}