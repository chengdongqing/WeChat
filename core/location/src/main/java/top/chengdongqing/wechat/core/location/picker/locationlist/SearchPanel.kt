package top.chengdongqing.wechat.core.location.picker.locationlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import top.chengdongqing.wechat.core.designsystem.components.searchbar.WeSearchBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.LaunchedUpdateEffect
import top.chengdongqing.wechat.core.designsystem.util.rememberKeyboardHeight
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.location.model.LocationInfo
import top.chengdongqing.wechat.core.location.picker.LocationPickerState
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun SearchPanel(state: LocationPickerState) {
    val listState = rememberLazyListState()
    val paging = state.pagingOfSearch

    val keywordFlow = remember { MutableStateFlow("") }
    val keyword by keywordFlow.collectAsState()
    var type by remember { mutableIntStateOf(0) }

    SearchingEffect(keywordFlow, state, paging, listState, keyword, type)
    KeyboardEffect(state)

    Column {
        WeSearchBar(
            value = keyword,
            modifier = Modifier.padding(16.dp),
            focused = true,
            onFocusChange = { if (!it) state.isSearchMode = false }
        ) {
            keywordFlow.value = it
        }
        TypeTabRow(type) { type = it }

        LocationList(
            listState = listState,
            pagingState = paging,
            selectedIndex = state.selectedIndexOfSearch,
            onSelect = { index ->
                state.selectedIndexOfSearch = index
                val latLng = paging.dataList[index].coordinate
                state.mapController.moveTo(latLng)
                state.isListExpanded = false
            }
        ) {
            if (!paging.isAllLoaded && !paging.isLoading) {
                val pageNum = paging.startLoadMore()
                state.search(
                    location = if (type == 0) state.currentLatLng else null,
                    keyword = keyword,
                    pageNum = pageNum
                ).onSuccess { items ->
                    val existing = paging.dataList.mapTo(HashSet()) { it.name }
                    paging.endLoadMore(items.filter { it.name !in existing })
                }.onFailure {
                    paging.cancelLoad()
                }
            }
        }
    }
}

@Composable
private fun TypeTabRow(type: Int, onChange: (Int) -> Unit) {
    val resources = LocalResources.current
    val options = remember {
        listOf(
            resources.getString(DesignR.string.location_tab_nearby),
            resources.getString(DesignR.string.location_tab_all)
        )
    }
    val density = LocalDensity.current
    val itemWidths = remember { mutableStateListOf(*Array(options.size) { 0.dp }) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            options.forEachIndexed { index, item ->
                val active = index == type
                Text(
                    text = item,
                    color = if (active) WeTheme.colorScheme.primary else WeTheme.colorScheme.textPrimary,
                    modifier = Modifier
                        .onSizeChanged { itemWidths[index] = with(density) { it.width.toDp() } }
                        .weClickable { onChange(index) }
                        .padding(vertical = 3.dp)
                )
            }
        }

        val targetOffsetX = remember(type, itemWidths.toList()) {
            var offset = 0.dp
            for (i in 0 until type) offset += itemWidths[i] + 16.dp
            offset
        }
        val animatedOffsetX by animateDpAsState(targetValue = targetOffsetX, label = "TabIndicator")
        val animatedWidth by animateDpAsState(
            targetValue = itemWidths.getOrElse(type) { 0.dp },
            label = "TabIndicatorWidth"
        )

        HorizontalDivider(
            modifier = Modifier
                .width(animatedWidth)
                .offset(x = animatedOffsetX),
            thickness = 2.dp,
            color = WeTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun SearchingEffect(
    keywordFlow: Flow<String>,
    state: LocationPickerState,
    paging: PagingState<LocationInfo>,
    listState: LazyListState,
    keyword: String,
    type: Int
) {
    val currentKeyword by rememberUpdatedState(keyword)
    val currentType by rememberUpdatedState(type)

    LaunchedEffect(keywordFlow) {
        keywordFlow
            .debounce(300)
            .filter { it.isNotBlank() }
            .collect { refresh(state, paging, listState, it, currentType) }
    }
    LaunchedUpdateEffect(currentType) {
        refresh(state, paging, listState, currentKeyword, currentType)
    }
}

private suspend fun refresh(
    state: LocationPickerState,
    paging: PagingState<LocationInfo>,
    listState: LazyListState,
    keyword: String,
    type: Int
) {
    state.selectedIndexOfSearch = null
    if (keyword.isBlank()) {
        paging.dataList = emptyList()
        return
    }
    paging.startRefresh()
    state.search(
        location = if (type == 0) state.currentLatLng else null,
        keyword = keyword
    ).onSuccess { items ->
        paging.endRefresh(items)
        listState.scrollToItem(0)
    }.onFailure {
        paging.cancelLoad()
    }
}

@Composable
private fun KeyboardEffect(state: LocationPickerState) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardHeight = rememberKeyboardHeight()

    LaunchedUpdateEffect(keyboardHeight) {
        if (keyboardHeight > 0.dp) state.isListExpanded = true
    }
    LaunchedUpdateEffect(state.isListExpanded) {
        if (!state.isListExpanded) keyboardController?.hide()
    }
}
