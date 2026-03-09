package top.chengdongqing.wechat.core.designsystem.components.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.app.model.AppItem
import top.chengdongqing.wechat.core.designsystem.components.app.model.AppResult
import top.chengdongqing.wechat.core.designsystem.components.app.model.toResult
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.contact.AlphabetIndexer
import top.chengdongqing.wechat.core.designsystem.components.contact.GroupTitle
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.util.showToast

@Composable
fun AppPicker(
    count: Int,
    onCancel: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel(),
    onSelect: (chatIds: Array<AppResult>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val overscrollEffect = rememberBounceOverscrollEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopBar(uiState, onBack = onCancel) {
                val selectedApps = uiState.selectedApps.toResult().toTypedArray()
                onSelect(selectedApps)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WeTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.overscroll(overscrollEffect),
                overscrollEffect = overscrollEffect
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when {
                    uiState.isLoading -> {
                        // 加载中
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                WeLoading()
                            }
                        }
                    }

                    uiState.groups.isEmpty() -> {
                        // 空状态
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无应用程序",
                                    color = WeTheme.colorScheme.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    else -> {
                        // apk分组列表
                        uiState.groups.forEach { (initial, contacts) ->
                            item(
                                key = initial,
                                contentType = "Initial"
                            ) {
                                GroupTitle(initial, background = WeTheme.colorScheme.surface)
                            }

                            itemsIndexed(
                                items = contacts,
                                key = { _, apk -> apk.packageName + apk.name },
                                contentType = { _, _ -> "ApkItem" }
                            ) { index, apk ->
                                val isSelected = viewModel.isSelected(apk)

                                Column(
                                    modifier = Modifier.background(WeTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.weClickable {
                                            if (uiState.selectedCount >= count && !isSelected) {
                                                context.showToast("你最多只能选择${count}个")
                                                return@weClickable
                                            }
                                            viewModel.toggleSelection(apk)
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        WeCheckBox(isSelected)
                                        ApkListItem(apk)
                                    }

                                    if (index < contacts.lastIndex) {
                                        WeDivider(modifier = Modifier.padding(start = 108.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // 右侧字母索引栏
            if (!uiState.isLoading && uiState.groups.isNotEmpty()) {
                AlphabetIndexer(uiState.groups) { initial ->
                    uiState.indexMap[initial]?.let { targetIndex ->
                        scope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApkListItem(apk: AppItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = apk.icon,
            contentDescription = null,
            error = painterResource(R.drawable.img_logo),
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = apk.name,
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = apk.packageName,
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TopBar(
    uiState: AppPickerUiState,
    onBack: () -> Unit,
    onOk: () -> Unit
) {
    val isEnabled = uiState.selectedCount > 0
    val buttonText = run {
        val suffix = if (isEnabled) "(${uiState.selectedCount})" else ""
        "完成$suffix"
    }

    WeTopBar(title = "选择应用程序", onBack = onBack) {
        WeButton(
            text = buttonText,
            size = ButtonSize.Small,
            enabled = isEnabled,
            onClick = onOk
        )
    }
}