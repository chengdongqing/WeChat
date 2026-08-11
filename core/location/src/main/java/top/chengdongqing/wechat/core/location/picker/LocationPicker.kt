package top.chengdongqing.wechat.core.location.picker

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.Black
import top.chengdongqing.wechat.core.file.createImageUri
import top.chengdongqing.wechat.core.location.LocationControl
import top.chengdongqing.wechat.core.location.WeMap
import top.chengdongqing.wechat.core.location.model.LocationInfo
import top.chengdongqing.wechat.core.location.picker.locationlist.SearchableLocationList
import top.chengdongqing.wechat.core.location.rememberMapController
import top.chengdongqing.wechat.core.location.repository.LocationRepository
import top.chengdongqing.wechat.core.location.util.createIconBitmap
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun WeLocationPicker(
    locationRepository: LocationRepository,
    onCancel: () -> Unit,
    onConfirm: (LocationInfo) -> Unit
) {
    val mapController = rememberMapController()
    val listState = rememberLazyListState()
    val pickerState = rememberLocationPickerState(mapController, locationRepository, listState)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val onConfirmClick = {
        scope.launch {
            isLoading = true
            try {
                var location = pickerState.selectedLocation!!
                val icon = createIconBitmap(context, DesignR.drawable.ic_location_marker, 160, 160)
                val bitmap = try {
                    mapController.takeSnapshot(location.coordinate, icon)
                } finally {
                    icon?.recycle()
                }

                if (bitmap != null) {
                    try {
                        val snapshot = context.createImageUri(bitmap)
                        location = location.copy(staticMapUri = snapshot)
                    } finally {
                        bitmap.recycle()
                    }
                }
                onConfirm(location)
            } finally {
                // 无论截图成功与否，都重置加载状态
                isLoading = false
            }
        }
        Unit
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            WeMap(controller = mapController) { controller ->
                LocationControl(controller) { point ->
                    pickerState.mapCenterLatLng = point
                }
            }
            TopBar(
                hasSelected = pickerState.selectedLocation != null,
                isLoading = isLoading,
                onCancel = onCancel,
                onConfirm = onConfirmClick
            )
            LocationMarker(pickerState)
        }
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            SearchableLocationList(pickerState, listState)
        }
    }
}

@Composable
private fun BoxScope.LocationMarker(state: LocationPickerState) {
    if (!state.isSearchMode || state.selectedLocation != null) {
        val offsetY = remember { Animatable(0f) }
        val animationSpec = remember { TweenSpec<Float>(durationMillis = 300) }

        LaunchedEffect(state.mapCenterLatLng) {
            offsetY.animateTo(-10f, animationSpec)
            offsetY.animateTo(0f, animationSpec)
        }
        Image(
            painter = painterResource(id = DesignR.drawable.ic_location_marker),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(50.dp)
                .offset {
                    val yOffsetDp = ((-25).dp + offsetY.value.dp)
                    IntOffset(x = 0, y = yOffsetDp.roundToPx())
                }
        )
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
            text = stringResource(DesignR.string.action_cancel),
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.onTap { onCancel() }
        )
        WeButton(
            text = stringResource(DesignR.string.action_done),
            size = ButtonSize.Small,
            enabled = hasSelected,
            loading = isLoading,
            onClick = onConfirm
        )
    }
}
