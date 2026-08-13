package top.chengdongqing.wechat.feature.moments.ui.post

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import top.chengdongqing.wechat.core.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetManager
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastManager
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import top.chengdongqing.wechat.core.media.picker.MediaPickerRequest
import top.chengdongqing.wechat.core.media.picker.rememberMediaPickerLauncher
import top.chengdongqing.wechat.feature.moments.R
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun PostMomentRoute(
    onBack: () -> Unit,
    viewModel: PostMomentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaPicker = rememberMediaPickerLauncher { result ->
        viewModel.addSelectedMedia(result.items)
    }
    val camera = rememberCameraLauncher { uri, type ->
        viewModel.addCapturedMedia(uri, type == VisualMediaType.Image)
    }
    val mediaOptions = remember {
        listOf(
            ActionSheetItem(R.string.moments_post_camera),
            ActionSheetItem(R.string.moments_post_gallery)
        )
    }

    LaunchedEffect(viewModel, onBack) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PostMomentEvent.Published -> onBack()
                is PostMomentEvent.Message -> ToastManager.fail(event.text)
            }
        }
    }

    val editingVideo = state.media as? PostMomentMedia.EditingVideo
    if (editingVideo != null) {
        MomentVideoEditor(
            source = editingVideo.source,
            onCancel = viewModel::cancelVideoEditing,
            onComplete = viewModel::completeVideoEditing
        )
        return
    }

    PostMomentScreen(
        state = state,
        onBack = onBack,
        onContentChange = viewModel::updateContent,
        onAddMedia = {
            ActionSheetManager.show(mediaOptions) { index ->
                when (index) {
                    0 -> camera(VisualMediaType.ImageAndVideo)
                    1 -> mediaPicker.launch(
                        MediaPickerRequest(
                            mediaType = VisualMediaType.ImageAndVideo,
                            maxSelection = state.remainingImageCount.coerceAtLeast(1)
                        )
                    )
                }
            }
        },
        onPublish = viewModel::publish
    )
}

@Composable
private fun PostMomentScreen(
    state: PostMomentUiState = PostMomentUiState(),
    onBack: () -> Unit = {},
    onContentChange: (String) -> Unit = {},
    onAddMedia: () -> Unit = {},
    onPublish: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            WeTopAppBar(
                containerColor = WeTheme.colorScheme.surface,
                backText = stringResource(DesignR.string.action_cancel),
                onBack = onBack
            ) {
                WeButton(
                    text = stringResource(R.string.moments_post_publish),
                    size = ButtonSize.Small,
                    enabled = state.canPublish,
                    onClick = onPublish
                )
            }
        },
        containerColor = WeTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBouncedOverscrollEffect()
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            WeInput(
                value = state.content,
                placeholder = stringResource(R.string.moments_post_hint),
                showDivider = false,
                singleLine = false,
                onValueChange = onContentChange
            )

            Spacer(Modifier.height(32.dp))
            MomentMediaSection(
                media = state.media,
                canAddMedia = state.remainingImageCount > 0,
                onAddMedia = onAddMedia
            )

            Spacer(Modifier.height(72.dp))
            MomentSettingsSection()
        }
    }
}

@Composable
private fun MomentMediaSection(
    media: PostMomentMedia,
    canAddMedia: Boolean,
    onAddMedia: () -> Unit
) {
    when (media) {
        PostMomentMedia.Empty -> MediaGrid(emptyList(), true, onAddMedia)
        is PostMomentMedia.Images -> MediaGrid(media.uris, canAddMedia, onAddMedia)
        is PostMomentMedia.Video -> MediaThumbnail(media.uri)
        is PostMomentMedia.EditingVideo -> Unit
    }
}

@Composable
@OptIn(ExperimentalGridApi::class)
private fun MediaGrid(
    images: List<Uri>,
    showAddButton: Boolean,
    onAddMedia: () -> Unit
) {
    Grid(
        modifier = Modifier.fillMaxWidth(),
        config = {
            repeat(3) { column(minmax(0.dp, 1.fr)) }
            gap(8.dp)
        }
    ) {
        images.forEach { uri ->
            MediaThumbnail(uri)
        }
        if (showAddButton) {
            AddMediaButton(
                onClick = onAddMedia
            )
        }
    }
}

@Composable
private fun AddMediaButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(WeTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_plus_outlined),
            contentDescription = stringResource(R.string.moments_post_add_media),
            modifier = Modifier.size(36.dp),
            tint = WeTheme.colorScheme.textSecondary
        )
    }
}

@Composable
private fun MediaThumbnail(uri: Uri) {
    Box(modifier = Modifier.aspectRatio(1f)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun MomentSettingsSection() {
    WeDivider()
    MomentSettingRow(
        title = stringResource(R.string.moments_post_location),
        icon = DesignR.drawable.ic_location_outlined,
        onClick = {}
    )
    WeDivider()
    MomentSettingRow(
        title = stringResource(R.string.moments_post_mention),
        icon = DesignR.drawable.ic_at_outlined,
        onClick = {}
    )
    WeDivider()
    MomentSettingRow(
        title = stringResource(R.string.moments_post_visibility),
        icon = DesignR.drawable.ic_tab_me_outlined,
        value = stringResource(R.string.moments_post_public),
        onClick = {}
    )
    WeDivider()
}

@Composable
private fun MomentSettingRow(
    title: String,
    icon: Int,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp)
        value?.let {
            Text(it, color = WeTheme.colorScheme.textSecondary, fontSize = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(DesignR.drawable.ic_right_outlined),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = WeTheme.colorScheme.textSecondary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostMomentScreenPreview() {
    WeTheme {
        PostMomentScreen()
    }
}
