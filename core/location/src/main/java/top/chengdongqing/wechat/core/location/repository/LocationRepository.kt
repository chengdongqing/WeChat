package top.chengdongqing.wechat.core.location.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.location.model.AddressInfo
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationInfo

interface LocationRepository {

    /** 坐标转地址（逆地理编码），null 表示无结果 */
    suspend fun locationToAddress(point: GeoPoint): AddressInfo?

    /**
     * POI 搜索
     *
     * @return [Result.success] 含结果列表；[Result.failure] 表示请求失败，
     *         便于调用方区分"搜索出错"与"没有结果"
     */
    suspend fun search(
        location: GeoPoint?,
        keyword: String,
        pageNum: Int,
        pageSize: Int,
        currentLocation: GeoPoint?
    ): Result<List<LocationInfo>>

    /** 上次缓存的设备坐标，进入选择器时用于快速恢复地图位置 */
    val currentLocation: Flow<GeoPoint?>

    suspend fun saveCurrentLocation(point: GeoPoint)
}
