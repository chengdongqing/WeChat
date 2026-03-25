package top.chengdongqing.wechat.core.location.picker.locationlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadMoreType
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.location.model.LocationInfo
import top.chengdongqing.wechat.core.location.util.formatDistance
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun LocationList(
    listState: LazyListState,
    pagingState: PagingState<LocationInfo>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onLoadMore: suspend () -> Unit
) {
    val loadMoreState = rememberLoadMoreState(
        enabled = { !pagingState.isAllLoaded },
        onLoadMore
    )

    Box(modifier = Modifier.nestedScroll(loadMoreState.nestedScrollConnection)) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(
                pagingState.dataList,
                key = { _, item -> item.id ?: item.name }
            ) { index, item ->
                LocationListItem(index == selectedIndex, item) {
                    onSelect(index)
                }
                if (index < pagingState.dataList.lastIndex) {
                    WeDivider()
                }
            }
            item {
                if (loadMoreState.isLoadingMore) {
                    WeLoadMore(listState = listState)
                } else if (pagingState.isAllLoaded) {
                    WeLoadMore(type = LoadMoreType.AllLoaded)
                }
            }
        }

        if (pagingState.isLoading && !loadMoreState.isLoadingMore) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                WeLoading(size = 80.dp)
            }
        }
    }
}

@Composable
private fun LocationListItem(checked: Boolean, location: LocationInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weClickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.name,
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildList {
                    location.distanceMetres?.let { add(formatDistance(it)) }
                    location.address?.let { add(it) }
                }.joinToString(" | "),
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 14.sp
            )
        }
        if (checked) {
            Icon(
                painter = painterResource(id = DesignR.drawable.ic_check),
                contentDescription = null,
                tint = WeTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}