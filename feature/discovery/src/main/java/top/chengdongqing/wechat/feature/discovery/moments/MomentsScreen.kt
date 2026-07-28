package top.chengdongqing.wechat.feature.discovery.moments

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.media.model.MediaItem
import top.chengdongqing.wechat.core.common.media.model.MediaType
import top.chengdongqing.wechat.core.common.media.preview.WeMediaPreview
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.StatusBarAppearanceEffect
import top.chengdongqing.wechat.core.designsystem.util.StatusBarVisibilityEffect
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MomentsScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onChangeCover: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var commentTarget by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var coverExpanded by remember { mutableStateOf(false) }
    var mediaPreview by remember { mutableStateOf<MomentMediaPreview?>(null) }
    var mediaPreviewClosing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val fadeDistancePx = with(LocalDensity.current) { 120.dp.toPx() }
    val topBarProgress by remember(listState, fadeDistancePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / fadeDistancePx).coerceIn(0f, 1f)
            }
        }
    }
    val topBarBackground = WeTheme.colorScheme.background.copy(alpha = topBarProgress)
    val topBarContentColor = lerp(Color.White, WeTheme.colorScheme.textPrimary, topBarProgress)
    StatusBarAppearanceEffect(isDark = topBarProgress >= 0.55f)
    StatusBarVisibilityEffect(visible = !coverExpanded)
    val mediaPreviewScope = rememberCoroutineScope()
    val closeMediaPreview: () -> Unit = {
        if (!mediaPreviewClosing) {
            mediaPreviewClosing = true
            mediaPreviewScope.launch {
                withFrameNanos { }
                withFrameNanos { }
                mediaPreview = null
                mediaPreviewClosing = false
            }
        }
    }
    SharedTransitionLayout {
        AnimatedContent(
            targetState = mediaPreview,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "moments-media-preview"
        ) { preview ->
            CompositionLocalProvider(
                LocalMomentsSharedTransitionScope provides this@SharedTransitionLayout,
                LocalMomentsAnimatedVisibilityScope provides this@AnimatedContent
            ) {
                if (preview == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(WeTheme.colorScheme.surface)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                MomentsHeader(
                                    cover = state.coverFor(viewModel.profile.id),
                                    name = viewModel.profile.nickname,
                                    avatar = viewModel.profile.avatarPath,
                                    expanded = coverExpanded,
                                    onCoverClick = { coverExpanded = !coverExpanded },
                                    onChangeCover = onChangeCover
                                )
                                Spacer(Modifier.height(32.dp))
                            }
                            items(state.moments, key = { it.id }) { moment ->
                                MomentItem(
                                    moment = moment,
                                    myId = viewModel.profile.id,
                                    onLike = { viewModel.toggleLike(moment.id) },
                                    onComment = { commentTarget = moment.id },
                                    onDelete = { deleteTarget = moment.id },
                                    onVideoClick = {
                                        moment.video?.let { video ->
                                            mediaPreview = MomentMediaPreview(
                                                moment.id,
                                                listOf(MomentPreviewImage(
                                                    video.path, video.width, video.height,
                                                    MediaType.Video, video.duration
                                                )),
                                                0
                                            )
                                        }
                                    },
                                    onImageClick = { index ->
                                        mediaPreviewClosing = false
                                        mediaPreviewScope.launch {
                                            mediaPreview = createMomentMediaPreview(
                                                moment.id,
                                                moment.images,
                                                index
                                            )
                                        }
                                    }
                                )
                            }
                            item { Spacer(Modifier.height(48.dp)) }
                        }
                        AnimatedVisibility(
                            visible = !coverExpanded,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            WeTopAppBar(
                                title = "朋友圈",
                                onBack = onBack,
                                containerColor = topBarBackground,
                                contentColor = topBarContentColor
                            ) {
                                IconButton(
                                    icon = R.drawable.ic_camera_filled,
                                    description = "发表朋友圈",
                                    onClick = onCreate
                                )
                            }
                        }
                    }
                } else {
                    BackHandler { closeMediaPreview() }
                    WeMediaPreview(
                        medias = preview.images.map { image ->
                            MediaItem(
                                uri = Uri.fromFile(File(image.path)),
                                filename = File(image.path).name,
                                mediaType = image.mediaType,
                                mimeType = if (image.mediaType == MediaType.Video) "video/*" else "image/*",
                                width = image.width,
                                height = image.height,
                                duration = image.duration
                            )
                        },
                        current = preview.initialIndex,
                        interactiveContentEnabled = !mediaPreviewClosing,
                        interactiveContentDelayMillis = 320L,
                        pageModifier = { index ->
                            Modifier.momentsMediaSharedElement(preview.momentId, index)
                        },
                        onDismiss = closeMediaPreview
                    )
                }
            }
        }
    }

    commentTarget?.let { id ->
        TextInputDialog("评论", "说点什么…", { commentTarget = null }) {
            viewModel.comment(id, it)
            commentTarget = null
        }
    }
    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这条朋友圈？") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(id); deleteTarget = null }) {
                    Text("删除", color = Color(0xFFE64340))
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun MomentsHeader(
    cover: String?,
    name: String,
    avatar: String?,
    expanded: Boolean,
    onCoverClick: () -> Unit,
    onChangeCover: () -> Unit
) {
    val expandedHeight = LocalWindowInfo.current.containerDpSize.height
    val headerHeight by animateDpAsState(
        targetValue = if (expanded) expandedHeight else 290.dp,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
        label = "moments-cover-height"
    )
    val isLandscape = remember(cover) {
        cover?.let { path ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            options.outWidth > options.outHeight && options.outHeight > 0
        } ?: false
    }
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(headerHeight)
            .background(Color(0xFF3D4652)).clickable(onClick = onCoverClick)
    ) {
        if (cover != null) {
            if (expanded && isLandscape) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(34.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.72f
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.46f)
                            )
                        )
                    )
                )
            }
            AsyncImage(
                model = cover,
                contentDescription = "朋友圈封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = if (expanded) ContentScale.Fit else ContentScale.Crop
            )
        }
        if (expanded) {
            Button(
                onClick = onChangeCover,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xB3333333)
                ),
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp)
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_camera_filled),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(7.dp))
                Text("换封面", color = Color.White)
            }
        }
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Row(
                modifier = Modifier.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.width(12.dp))
                AsyncImage(
                    model = avatar ?: R.drawable.img_avatar_placeholder,
                    contentDescription = null,
                    modifier = Modifier.size(74.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun MomentItem(
    moment: Moment,
    myId: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onDelete: () -> Unit,
    onVideoClick: () -> Unit,
    onImageClick: (Int) -> Unit
) {
    var actionsExpanded by remember(moment.id) { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
        AsyncImage(
            model = moment.authorAvatar ?: R.drawable.img_avatar_placeholder,
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(moment.authorName, color = Color(0xFF576B95), fontWeight = FontWeight.Bold)
            if (moment.content.isNotBlank()) {
                Text(moment.content, modifier = Modifier.padding(top = 5.dp), fontSize = 16.sp)
            }
            if (moment.images.isNotEmpty()) {
                MomentImages(moment.id, moment.images, onImageClick)
            }
            moment.video?.let { video ->
                Box(
                    modifier = Modifier.padding(top = 8.dp)
                        .size(width = 210.dp, height = 280.dp)
                        .clip(RoundedCornerShape(3.dp)).background(Color.Black)
                        .clickable(onClick = onVideoClick),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = video.thumbnailPath ?: video.path,
                        contentDescription = "视频",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Text("▶", color = Color.White, fontSize = 38.sp)
                    Text(
                        formatDuration(video.duration),
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .background(Color(0x66000000)).padding(6.dp, 3.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatTime(moment.createdAt), color = Color.Gray, fontSize = 12.sp)
                if (moment.authorId == myId) {
                    Text("  删除", color = Color(0xFF576B95), fontSize = 12.sp,
                        modifier = Modifier.clickable(onClick = onDelete))
                }
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(
                    visible = actionsExpanded,
                    enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF4C5154))
                    ) {
                        Text(
                            if (moment.likes.any { it.userId == myId }) "♡ 取消" else "♡ 赞",
                            color = Color.White,
                            modifier = Modifier.clickable {
                                actionsExpanded = false
                                onLike()
                            }.padding(14.dp, 8.dp),
                            fontSize = 13.sp
                        )
                        Text("│", color = Color(0xFF73787B), modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            "◌ 评论",
                            color = Color.White,
                            modifier = Modifier.clickable {
                                actionsExpanded = false
                                onComment()
                            }.padding(14.dp, 8.dp),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_more_outlined),
                    contentDescription = "赞和评论",
                    tint = Color(0xFF576B95),
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF3F3F5))
                        .clickable { actionsExpanded = !actionsExpanded }
                        .padding(5.dp)
                )
            }
            if (moment.likes.isNotEmpty() || moment.comments.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(Color(0xFFF3F3F5)).padding(9.dp)
                ) {
                    if (moment.likes.isNotEmpty()) {
                        Text("♥  " + moment.likes.joinToString("，") { it.userName },
                            color = Color(0xFF576B95), fontSize = 14.sp)
                    }
                    moment.comments.forEach {
                        Text("${it.userName}：${it.content}", fontSize = 14.sp,
                            modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentImages(
    momentId: String,
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    val shown = images.take(9)
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        shown.withIndex().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { indexedPath ->
                    val index = indexedPath.index
                    val path = indexedPath.value
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        modifier = Modifier
                            .momentsMediaSharedElement(momentId, index)
                            .size(if (shown.size == 1) 190.dp else 92.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .clickable { onImageClick(index) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

private data class MomentMediaPreview(
    val momentId: String,
    val images: List<MomentPreviewImage>,
    val initialIndex: Int
)

private data class MomentPreviewImage(
    val path: String,
    val width: Int,
    val height: Int,
    val mediaType: MediaType = MediaType.Image,
    val duration: Long = 0
)

private suspend fun createMomentMediaPreview(
    momentId: String,
    images: List<String>,
    initialIndex: Int
): MomentMediaPreview = withContext(Dispatchers.IO) {
    MomentMediaPreview(
        momentId = momentId,
        images = images.map { path ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            MomentPreviewImage(
                path = path,
                width = options.outWidth.coerceAtLeast(1),
                height = options.outHeight.coerceAtLeast(1)
            )
        },
        initialIndex = initialIndex
    )
}

private fun formatTime(time: Long): String =
    SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(time))

private fun formatDuration(duration: Long): String =
    "%d:%02d".format(duration / 60_000, duration / 1_000 % 60)

@Composable
private fun TextInputDialog(
    title: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text("发表") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
