package top.chengdongqing.wechat.core.location.geocoding

import top.chengdongqing.wechat.core.location.model.AddressInfo
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationInfo

/**
 * 地理编码数据源抽象接口
 */
interface GeocodingDataSource {

    /**
     * 坐标转地址（逆地理编码）
     *
     * @return null 表示无结果，[Result.failure] 表示请求失败
     */
    suspend fun reverseGeocode(point: GeoPoint): AddressInfo?

    /**
     * POI 搜索
     *
     * @param center 搜索中心点，null 时进行全局搜索
     * @param currentLocation 当前设备坐标，用于计算各 POI 距当前位置的距离
     * @return [Result.success] 含结果列表（可为空）；[Result.failure] 表示请求失败
     */
    suspend fun searchPOI(
        center: GeoPoint?,
        keyword: String,
        pageNum: Int,
        pageSize: Int,
        currentLocation: GeoPoint?
    ): Result<List<LocationInfo>>
}
