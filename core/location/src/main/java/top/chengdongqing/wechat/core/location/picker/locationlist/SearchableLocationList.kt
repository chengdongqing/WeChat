package top.chengdongqing.wechat.core.location.picker.locationlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.searchbar.WeSearchBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.location.picker.LocationPickerState
import top.chengdongqing.wechat.core.location.R as LocationR

@Composable
fun SearchableLocationList(state: LocationPickerState, listState: LazyListState) {
    val animatedHeightFraction = animateFloatAsState(
        targetValue = if (state.isListExpanded) 0.7f else 0.4f,
        label = "LocationListHeightAnimation"
    )
    val nestedScrollConnection = remember(state) {
        LocationListNestedScrollConnection(state, animatedHeightFraction)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(animatedHeightFraction.value)
            .expandedStyle(state.isListExpanded)
            .background(WeTheme.colorScheme.surface)
            .nestedScroll(nestedScrollConnection)
    ) {
        if (state.isListExpanded) TopArrow(state)

        if (state.isSearchMode) {
            SearchPanel(state)
        } else {
            SearchInput(state)
            LocationList(
                listState = listState,
                pagingState = state.paging,
                selectedIndex = state.selectedIndex,
                onSelect = { index ->
                    state.selectedIndex = index
                    val point = state.paging.dataList[index].coordinate
                    state.mapController.moveTo(point)
                }
            ) {
                if (!state.paging.isAllLoaded && !state.paging.isLoading) {
                    val pageNum = state.paging.startLoadMore()
                    state.search(state.mapCenterLatLng, pageNum = pageNum)
                        .onSuccess { items ->
                            val existing = state.paging.dataList.mapTo(HashSet()) { it.name }
                            state.paging.endLoadMore(items.filter { it.name !in existing })
                        }
                        .onFailure { state.paging.cancelLoad() }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TopArrow(state: LocationPickerState) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(WeTheme.colorScheme.background)
            .clickable { state.isListExpanded = false }
            .padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = WeTheme.colorScheme.textSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun Modifier.expandedStyle(expanded: Boolean) = if (expanded) {
    this
        .offset(y = (-12).dp)
        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
} else {
    this
}

@Composable
private fun SearchInput(state: LocationPickerState) {
    WeSearchBar(
        value = "",
        modifier = Modifier.padding(16.dp),
        placeholder = stringResource(LocationR.string.location_search_placeholder),
        disabled = true,
        onClick = { state.isSearchMode = true }
    ) {}
}

private class LocationListNestedScrollConnection(
    private val state: LocationPickerState,
    private val heightFraction: State<Float>
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y < 0 && !state.isListExpanded) state.isListExpanded = true
        // 动画已完成展开时才消费滚动
        return if (heightFraction.value >= 0.7f) Offset.Zero else available
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (available.y > 0 && state.isListExpanded) state.isListExpanded = false
        return Offset.Zero
    }
}
