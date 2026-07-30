package top.chengdongqing.wechat.core.location.picker

import android.view.MotionEvent
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.location.map.MapController
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.LocationInfo
import top.chengdongqing.wechat.core.location.picker.locationlist.PagingState
import top.chengdongqing.wechat.core.location.picker.locationlist.PagingStateImpl
import top.chengdongqing.wechat.core.location.repository.LocationRepository

@Stable
interface LocationPickerState {
    val mapController: MapController
    val currentLatLng: GeoPoint?
    var mapCenterLatLng: GeoPoint?
    var isSearchMode: Boolean
    var isListExpanded: Boolean
    val paging: PagingState<LocationInfo>
    var selectedIndex: Int
    val pagingOfSearch: PagingState<LocationInfo>
    var selectedIndexOfSearch: Int?

    /**
     * 当前选中的位置，普通模式取 [paging]，搜索模式取 [pagingOfSearch]
     */
    val selectedLocation: LocationInfo?

    /**
     * 搜索 POI
     *
     * @return [Result.success] 含结果列表；[Result.failure] 表示请求失败
     */
    suspend fun search(
        location: GeoPoint?,
        keyword: String = "",
        pageNum: Int = 1,
        pageSize: Int = 10
    ): Result<List<LocationInfo>>
}

@Composable
fun rememberLocationPickerState(
    mapController: MapController,
    locationRepository: LocationRepository,
    listState: LazyListState
): LocationPickerState {
    val scope = rememberCoroutineScope()
    val currentLocation = locationRepository.currentLocation.collectAsState(initial = null)

    val state = remember {
        LocationPickerStateImpl(
            mapController,
            locationRepository,
            scope,
            currentLocation,
            listState
        )
    }

    // 恢复上次缓存的坐标，快速还原地图位置
    LaunchedEffect(currentLocation.value) {
        currentLocation.value?.let {
            if (state.mapCenterLatLng == null) {
                mapController.moveTo(it)
                state.mapCenterLatLng = it
            }
        }
    }

    return state
}

private class LocationPickerStateImpl(
    override val mapController: MapController,
    private val locationRepository: LocationRepository,
    private val scope: CoroutineScope,
    private val _currentLatLng: State<GeoPoint?>,
    private val listState: LazyListState
) : LocationPickerState {

    // getter 读缓存 flow，setter 只负责持久化到 DataStore
    override var currentLatLng: GeoPoint?
        get() = _currentLatLng.value
        set(value) {
            value?.let {
                scope.launch {
                    locationRepository.saveCurrentLocation(it)
                }
            }
        }

    override var mapCenterLatLng: GeoPoint?
        get() = _mapCenterLatLng
        set(value) {
            _mapCenterLatLng = value
            if (isSearchMode) isSearchMode = false
            if (value == null) return

            selectedIndex = 0
            paging.startRefresh()
            scope.launch {
                // 地址解析和附近 POI 是两个独立网络请求，并行可明显缩短首屏等待。
                val (centerItem, nearby) = coroutineScope {
                    val centerDeferred = async { reverseGeocodeToItem(value) }
                    val nearbyDeferred = async { search(value).getOrElse { emptyList() } }
                    (centerDeferred.await()?.let(::listOf) ?: emptyList()) to nearbyDeferred.await()
                }
                paging.endRefresh(centerItem + nearby)
                listState.scrollToItem(0)
            }
        }

    override var isSearchMode: Boolean
        get() = _isSearchMode
        set(value) {
            _isSearchMode = value
            isListExpanded = value
            if (!value) {
                // 退出搜索：清空搜索结果，恢复地图视野到普通模式中心点
                pagingOfSearch.dataList = emptyList()
                _mapCenterLatLng?.let { mapController.moveTo(it) }
            }
        }

    override var isListExpanded by mutableStateOf(false)
    override val paging = PagingStateImpl<LocationInfo>(initialLoading = true)
    override var selectedIndex by mutableIntStateOf(0)
    override val pagingOfSearch = PagingStateImpl<LocationInfo>()
    override var selectedIndexOfSearch by mutableStateOf<Int?>(null)

    override val selectedLocation: LocationInfo?
        get() = if (!isSearchMode) {
            paging.dataList.getOrNull(selectedIndex)
        } else {
            selectedIndexOfSearch?.let { pagingOfSearch.dataList.getOrNull(it) }
        }

    private var _mapCenterLatLng by mutableStateOf<GeoPoint?>(null)
    private var _isSearchMode by mutableStateOf(false)

    override suspend fun search(
        location: GeoPoint?,
        keyword: String,
        pageNum: Int,
        pageSize: Int
    ): Result<List<LocationInfo>> {
        if (location == null && keyword.isBlank()) return Result.success(emptyList())
        return locationRepository.search(location, keyword, pageNum, pageSize, currentLatLng)
    }

    init {
        setupMapListeners()
    }

    /**
     * 将坐标逆解析为 LocationInfo（作为列表第一项展示在附近搜索结果之前）
     */
    private suspend fun reverseGeocodeToItem(point: GeoPoint): LocationInfo? {
        return locationRepository.locationToAddress(point)?.let { info ->
            val startIndex = info.formattedAddress.lastIndexOf(info.district).coerceAtLeast(0)
            val name = info.formattedAddress.substring(startIndex)
            LocationInfo(name = name, address = info.formattedAddress, coordinate = point)
        }
    }

    private fun setupMapListeners() {
        mapController.setOnMapClickListener { point ->
            mapController.moveTo(point)
            mapCenterLatLng = point
        }
        mapController.setOnPoiClickListener { point ->
            mapController.moveTo(point)
            mapCenterLatLng = point
        }
        mapController.setOnTouchListener { event ->
            // 拖拽松手时，将地图当前中心点作为新选点
            if (event.action == MotionEvent.ACTION_UP && !isSearchMode) {
                mapController.cameraCenter?.let { mapCenterLatLng = it }
            }
        }
        mapController.setOnLocationChangeListener { point ->
            // 首次获得定位：移动地图并触发附近搜索
            if (currentLatLng == null && !isSearchMode) {
                mapController.moveTo(point)
                mapCenterLatLng = point
            }
            // 每次定位更新都持久化，方便下次快速恢复
            currentLatLng = point
        }
    }
}
