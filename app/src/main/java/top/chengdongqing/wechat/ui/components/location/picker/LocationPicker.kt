package top.chengdongqing.wechat.ui.components.location.picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.createBitmapDescriptor
import top.chengdongqing.wechat.core.util.createImageUri
import top.chengdongqing.wechat.data.model.LocationItem
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.components.location.AMap
import top.chengdongqing.wechat.ui.components.location.LocationControl
import top.chengdongqing.wechat.ui.components.location.picker.locationlist.SearchableLocationList
import top.chengdongqing.wechat.ui.components.location.rememberAMapState
import top.chengdongqing.wechat.ui.theme.Black
import top.chengdongqing.wechat.ui.util.weClickable

@Composable
fun WeLocationPicker(
    onCancel: () -> Unit,
    onConfirm: (LocationItem) -> Unit
) {
    val mapState = rememberAMapState()
    val listState = rememberLazyListState()
    val state = rememberLocationPickerState(mapState.map, listState)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    /**
     * 处理位置回调
     */
    val handleConfirm = {
        scope.launch {
            isLoading = true
            // 获取地图快照
            mapState.takeSnapshot(state.isSearchMode)?.let { bitmap ->
                // 保存到缓存并获取uri
                val snapshot = context.createImageUri(bitmap)
                val location = state.selectedLocation!!.copy(snapshotUri = snapshot)
                onConfirm(location)
                isLoading = false
            }
        }
        Unit
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AMap(state = mapState) { map ->
                LocationControl(map) {
                    state.mapCenterLatLng = it
                }
            }
            TopBar(
                hasSelected = state.selectedLocation != null,
                isLoading = isLoading,
                onCancel = onCancel,
                onConfirm = handleConfirm
            )
            LocationMarker(state)
        }
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            SearchableLocationList(state, listState)
        }
    }
}

@Composable
private fun BoxScope.LocationMarker(state: LocationPickerState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (!state.isSearchMode) {
        val offsetY = remember { Animatable(0f) }
        val animationSpec = remember { TweenSpec<Float>(durationMillis = 300) }

        LaunchedEffect(state.mapCenterLatLng) {
            offsetY.animateTo(-10f, animationSpec)
            offsetY.animateTo(0f, animationSpec)
        }
        Image(
            painter = painterResource(id = R.drawable.ic_location_marker),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(50.dp)
                .offset(y = (-25).dp + offsetY.value.dp)
        )
    } else if (state.selectedLocation != null) {
        DisposableEffect(state.selectedLocation) {
            val markerOptions = MarkerOptions().apply {
                position(state.selectedLocation?.latLng)
                scope.launch {
                    icon(
                        createBitmapDescriptor(
                            context,
                            R.drawable.ic_location_marker,
                            160,
                            160
                        )
                    )
                }
            }
            val marker = state.map.addMarker(markerOptions)

            onDispose {
                marker?.remove()
            }
        }
    }
}

@Composable
private fun TopBar(
    hasSelected: Boolean,
    isLoading: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Black.copy(alpha = 0.4f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "取消",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weClickable { onCancel() }
        )
        WeButton(
            text = "确定",
            size = ButtonSize.Small,
            disabled = !hasSelected,
            loading = isLoading,
            onClick = onConfirm
        )
    }
}