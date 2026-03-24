package top.chengdongqing.wechat.core.location.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.location.geocoding.GeocodingDataSource
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationInfo

class LocationRepositoryImpl @Inject constructor(
    private val dataSource: GeocodingDataSource,
    @param:ApplicationContext private val context: Context
) : LocationRepository {

    override suspend fun locationToAddress(point: GeoPoint) = dataSource.reverseGeocode(point)

    override suspend fun search(
        location: GeoPoint?,
        keyword: String,
        pageNum: Int,
        pageSize: Int,
        currentLocation: GeoPoint?
    ): Result<List<LocationInfo>> =
        dataSource.searchPOI(location, keyword, pageNum, pageSize, currentLocation)

    override val currentLocation: Flow<GeoPoint?>
        get() = context.locationDataStore.data.map { prefs ->
            val lat = prefs[PreferencesKeys.LATITUDE]
            val lng = prefs[PreferencesKeys.LONGITUDE]
            if (lat != null && lng != null) GeoPoint(lat, lng) else null
        }

    override suspend fun saveCurrentLocation(point: GeoPoint) {
        context.locationDataStore.edit { prefs ->
            prefs[PreferencesKeys.LATITUDE] = point.latitude
            prefs[PreferencesKeys.LONGITUDE] = point.longitude
        }
    }
}
