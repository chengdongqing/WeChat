package top.chengdongqing.wechat.core.media.picker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetManager
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoadMore
import top.chengdongqing.wechat.core.designsystem.components.permission.RequestMediaPermission
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.file.createImageUri
import top.chengdongqing.wechat.core.file.createVideoUri
import top.chengdongqing.wechat.core.file.deleteFileByUri
import top.chengdongqing.wechat.core.file.getFileMetadata
import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.core.media.model.MediaType
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import java.util.Locale
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.core.media.ui.R as MediaUiR

@Composable
fun WeMediaPicker(
    type: VisualMediaType,
    count: Int,
    enableMerge: Boolean = false,
    onCancel: () -> Unit,
    onConfirm: (Array<MediaItem>, merge: Boolean, original: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val singleMediaMode = count == 1
    val captureVideo = type == VisualMediaType.Video
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    var original by remember { mutableStateOf(false) }

    fun completeCapturedMedia(success: Boolean, uri: Uri?, mediaType: MediaType) {
        val actualUri = uri ?: return
        coroutineScope.launch {
            if (success) {
                context.getFileMetadata(actualUri)?.let { metadata ->
                    onConfirm(
                        arrayOf(
                            MediaItem(
                                uri = actualUri,
                                filename = metadata.filename,
                                mediaType = mediaType,
                                mimeType = metadata.mimeType,
                                width = metadata.width,
                                height = metadata.height,
                                size = metadata.size
                            )
                        ),
                        false,
                        false
                    )
                }
            } else {
                context.deleteFileByUri(actualUri)
            }
            capturedUri = null
        }
    }

    val takePicture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            completeCapturedMedia(success, capturedUri, MediaType.Image)
        }
    val captureVideoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            completeCapturedMedia(success, capturedUri, MediaType.Video)
        }

    fun launchCapture() {
        coroutineScope.launch {
            capturedUri = if (captureVideo) context.createVideoUri() else context.createImageUri()
            if (captureVideo) captureVideoLauncher.launch(capturedUri!!)
            else takePicture.launch(capturedUri!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.surfaceVariant)
    ) {
        RequestMediaPermission(onRevoked = onCancel) {
            val state = rememberMediaPickerState(type, count)

            previewIndex?.let { index ->
                PickerMediaPreview(
                    medias = state.mediaList,
                    initialIndex = index.coerceIn(0, state.mediaList.lastIndex.coerceAtLeast(0)),
                    state = state,
                    original = original,
                    onOriginalChange = { original = it },
                    onDismiss = { previewIndex = null },
                    onConfirm = {
                        onConfirm(state.selectedMediaList.toTypedArray(), false, original)
                    }
                )
                return@RequestMediaPermission
            }

            TopBar(state, onCancel)
            if (state.isLoading) {
                WeLoadMore()
            } else {
                MediaGrid(
                    state = state,
                    singleMediaMode = singleMediaMode,
                    captureVideo = captureVideo,
                    onCapture = ::launchCapture,
                    onMediaPreview = { previewIndex = it }
                )
                if (!singleMediaMode) {
                    BottomBar(
                        state = state,
                        enableMerge = enableMerge,
                        original = original,
                        onOriginalChange = { original = it },
                        onPreview = { previewIndex = it }
                    ) { merge ->
                        onConfirm(state.selectedMediaList.toTypedArray(), merge, original)
                    }
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
    val scope = rememberCoroutineScope()
    val typeOptions = remember {
        listOf(
            ActionSheetItem(MediaUiR.string.media_select_image, value = VisualMediaType.Image),
            ActionSheetItem(MediaUiR.string.media_select_video, value = VisualMediaType.Video),
            ActionSheetItem(MediaUiR.string.media_select_all, value = VisualMediaType.ImageAndVideo)
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
                    ActionSheetManager.show(typeOptions) { index ->
                        scope.launch {
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
                    contentDescription = stringResource(DesignR.string.action_more),
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
private fun BottomBar(
    state: MediaPickerState,
    enableMerge: Boolean,
    original: Boolean,
    onOriginalChange: (Boolean) -> Unit,
    onPreview: (Int) -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    val selectedCount = state.selectedMediaList.size
    val countDescription = if (selectedCount > 0) "($selectedCount)" else ""
    val selectedSize = state.selectedMediaList.sumOf(MediaItem::size)

    var merge by remember { mutableStateOf(false) }
    if (selectedCount < 3) merge = false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (enableMerge && selectedCount >= 3) {
            Row(
                modifier = Modifier
                    .clickable { merge = !merge }
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeCheckBox(checked = merge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "发送后合并展示",
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 16.sp
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "${stringResource(DesignR.string.action_preview)}$countDescription",
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .alpha(if (selectedCount > 0) 1f else 0.6f)
                    .clickable(enabled = selectedCount > 0) {
                        val firstSelected = state.mediaList.indexOf(state.selectedMediaList.first())
                        if (firstSelected >= 0) onPreview(firstSelected)
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable { onOriginalChange(!original) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WeCheckBox(checked = original)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("原图", color = WeTheme.colorScheme.textPrimary, fontSize = 16.sp)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "共 ${formatFileSize(selectedSize)}",
                        color = WeTheme.colorScheme.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.alpha(
                            if (original && selectedCount > 0) 1f else 0f
                        )
                    )
                }
            }
            WeButton(
                text = "${stringResource(DesignR.string.action_ok)}$countDescription",
                size = ButtonSize.Small,
                enabled = selectedCount > 0,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                onConfirm(merge)
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    val pattern = if (value >= 100 || value % 1.0 == 0.0) "%.0f" else "%.1f"
    return String.format(Locale.getDefault(), "$pattern %s", value, units[unitIndex])
}
