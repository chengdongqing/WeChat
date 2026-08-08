package top.chengdongqing.wechat.core.apppicker

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.apppicker.model.AppItem
import top.chengdongqing.wechat.core.apppicker.model.AppResult
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.contact.GroupTitle
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.indexer.AlphabetIndexer
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun AppPicker(
    count: Int,
    onCancel: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel(),
    onSelect: (chatIds: Array<AppResult>) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val overscrollEffect = rememberBouncedOverscrollEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopBar(uiState, onBack = onCancel) {
                scope.launch {
                    runCatching { viewModel.prepareSelectedApps() }
                        .onSuccess(onSelect)
                        .onFailure {
                            context.showToast(it.message ?: "应用文件准备失败")
                        }
                }
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .overscroll(overscrollEffect),
                overscrollEffect = overscrollEffect
            ) {
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

                    else -> {
                        item {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(WeTheme.colorScheme.surface)
                            )
                        }

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
                                        modifier = Modifier.onTap {
                                            if (uiState.selectedCount >= count && !isSelected) {
                                                context.showToast(
                                                    resources.getString(
                                                        DesignR.string.msg_max_select_limit,
                                                        count
                                                    )
                                                )
                                                return@onTap
                                            }
                                            viewModel.toggleSelection(apk)
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        WeCheckBox(isSelected)
                                        ApkListItem(
                                            apk = apk,
                                            icon = viewModel.iconFor(apk.packageName),
                                            onLoadIcon = viewModel::loadIcon
                                        )
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
private fun ApkListItem(
    apk: AppItem,
    icon: android.graphics.drawable.Drawable?,
    onLoadIcon: (String) -> Unit
) {
    LaunchedEffect(apk.packageName) {
        onLoadIcon(apk.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = icon,
            contentDescription = null,
            placeholder = painterResource(DesignR.drawable.img_logo),
            error = painterResource(DesignR.drawable.img_logo),
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
    val isEnabled = uiState.selectedCount > 0 && !uiState.isPreparing
    val buttonText = run {
        val suffix = if (isEnabled) "(${uiState.selectedCount})" else ""
        "${stringResource(DesignR.string.action_done)}$suffix"
    }

    WeTopAppBar(
        title = stringResource(DesignR.string.app_select_title),
        onBack = onBack
    ) {
        WeButton(
            text = buttonText,
            size = ButtonSize.Small,
            enabled = isEnabled,
            onClick = onOk
        )
    }
}
