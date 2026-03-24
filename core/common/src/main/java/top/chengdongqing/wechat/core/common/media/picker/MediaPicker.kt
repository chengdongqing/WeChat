package top.chengdongqing.wechat.core.common.media.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDownCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.R
import top.chengdongqing.wechat.core.common.media.model.MediaItem
import top.chengdongqing.wechat.core.common.media.model.VisualMediaType
import top.chengdongqing.wechat.core.common.media.preview.previewMedias
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.RequestMediaPermission

@Composable
fun WeMediaPicker(
    type: VisualMediaType,
    count: Int,
    onCancel: () -> Unit,
    onConfirm: (Array<MediaItem>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.surfaceVariant)
    ) {
        RequestMediaPermission(onRevoked = onCancel) {
            val state = rememberMediaPickerState(type, count)

            TopBar(state, onCancel)
            if (state.isLoading) {
                WeLoadMore()
            } else {
                MediaGrid(state)
                BottomBar(state) {
                    onConfirm(state.selectedMediaList.toTypedArray())
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    state: MediaPickerState,
    onCancel: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val actionSheet = rememberActionSheetState()
    val typeOptions = remember {
        listOf(
            ActionSheetItem(R.string.media_select_image, value = VisualMediaType.Image),
            ActionSheetItem(R.string.media_select_video, value = VisualMediaType.Video),
            ActionSheetItem(R.string.media_select_all, value = VisualMediaType.ImageAndVideo)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 6.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "返回",
            tint = WeTheme.colorScheme.textPrimary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 16.dp)
                .size(28.dp)
                .clickable {
                    onCancel()
                }
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(20.dp))
                .background(WeTheme.colorScheme.divider)
                .clickable(enabled = state.isTypeEnabled) {
                    actionSheet.show(typeOptions) { index ->
                        coroutineScope.launch {
                            state.refresh(typeOptions[index].value as VisualMediaType)
                        }
                    }
                }
                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(typeOptions.find { it.value == state.type }?.labelRes!!),
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 16.sp
            )
            if (state.isTypeEnabled) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowDropDownCircle,
                    contentDescription = stringResource(R.string.action_more),
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun BottomBar(state: MediaPickerState, onConfirm: () -> Unit) {
    val context = LocalContext.current
    val selectedCount = state.selectedMediaList.size
    val countDescription = if (selectedCount > 0) "($selectedCount)" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${stringResource(R.string.action_preview)}$countDescription",
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 16.sp,
            modifier = Modifier
                .alpha(if (selectedCount > 0) 1f else 0.6f)
                .clickable(enabled = selectedCount > 0) {
                    context.previewMedias(state.selectedMediaList)
                }
        )
        WeButton(
            text = "${stringResource(R.string.action_ok)}$countDescription",
            size = ButtonSize.Small,
            enabled = selectedCount > 0
        ) {
            onConfirm()
        }
    }
}