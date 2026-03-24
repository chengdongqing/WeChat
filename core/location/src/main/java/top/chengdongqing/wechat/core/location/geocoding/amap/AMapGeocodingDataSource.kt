package top.chengdongqing.wechat.core.location.geocoding.amap

import android.content.Context
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItemV2
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.poisearch.PoiResultV2
import com.amap.api.services.poisearch.PoiSearchV2
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.location.geocoding.GeocodingDataSource
import top.chengdongqing.wechat.core.location.map.amap.toGeoPoint
import top.chengdongqing.wechat.core.location.map.amap.toLatLng
import top.chengdongqing.wechat.core.location.map.amap.toLatLonPoint
import top.chengdongqing.wechat.core.location.model.AddressInfo
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationInfo
import kotlin.coroutines.resume
import kotlin.math.roundToInt

class AMapGeocodingDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : GeocodingDataSource {

    override suspend fun reverseGeocode(point: GeoPoint): AddressInfo? =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                val search = GeocodeSearch(context)
                search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                    override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                        val address = if (code == AMapException.CODE_AMAP_SUCCESS) {
                            result?.regeocodeAddress?.let {
                                AddressInfo(
                                    formattedAddress = it.formatAddress,
                                    district = it.district
                                )
                            }
                        } else null
                        cont.resume(address)
                    }

                    override fun onGeocodeSearched(result: GeocodeResult?, code: Int) {}
                })
                search.getFromLocationAsyn(
                    RegeocodeQuery(point.toLatLonPoint(), 100_000f, GeocodeSearch.AMAP)
                )
            }
        }

    override suspend fun searchPOI(
        center: GeoPoint?,
        keyword: String,
        pageNum: Int,
        pageSize: Int,
        currentLocation: GeoPoint?
    ): Result<List<LocationInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            suspendCancellableCoroutine { cont ->
                val query = PoiSearchV2.Query(keyword, "", "").apply {
                    this.pageNum = pageNum
                    this.pageSize = pageSize
                }
                val poiSearch = PoiSearchV2(context, query)
                if (center != null) {
                    poiSearch.bound = PoiSearchV2.SearchBound(center.toLatLonPoint(), 100_000)
                }
                poiSearch.setOnPoiSearchListener(object : PoiSearchV2.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResultV2?, code: Int) {
                        if (code == AMapException.CODE_AMAP_SUCCESS && result?.query != null) {
                            val items = result.pois.map { poi ->
                                LocationInfo(
                                    id = poi.poiId,
                                    name = poi.title,
                                    address = poi.adName,
                                    distanceMetres = currentLocation?.let {
                                        AMapUtils.calculateLineDistance(
                                            it.toLatLng(),
                                            poi.latLonPoint.toLatLng()
                                        ).roundToInt()
                                    },
                                    coordinate = poi.latLonPoint.toGeoPoint()
                                )
                            }
                            cont.resume(items)
                        } else {
                            cont.resumeWith(Result.failure(RuntimeException("POI 搜索失败，code=$code")))
                        }
                    }

                    override fun onPoiItemSearched(item: PoiItemV2?, code: Int) {}
                })
                poiSearch.searchPOIAsyn()
            }
        }
    }
}

private fun LatLonPoint.toLatLng() = LatLng(latitude, longitude)