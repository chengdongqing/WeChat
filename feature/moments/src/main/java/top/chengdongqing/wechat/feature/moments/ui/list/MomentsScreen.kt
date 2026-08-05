package top.chengdongqing.wechat.feature.moments.ui.list

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.media.model.MediaType
import top.chengdongqing.wechat.core.common.time.toRelativeDateTime
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryDark
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryLight
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.moments.model.Moment
import top.chengdongqing.wechat.feature.moments.model.coverFor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MomentsScreen(
    onBack: () -> Unit,
    onNavigateToPost: () -> Unit,
    onNavigateToCover: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
//    var commentTarget by remember { mutableStateOf<String?>(null) }
//    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var coverExpanded by remember { mutableStateOf(false) }
//    var mediaPreview by remember { mutableStateOf<MomentMediaPreview?>(null) }
//    var mediaPreviewClosing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

//    fun closeMediaPreview() {
//        if (!mediaPreviewClosing) {
//            mediaPreviewClosing = true
//            scope.launch {
//                withFrameNanos { }
//                withFrameNanos { }
//                mediaPreview = null
//                mediaPreviewClosing = false
//            }
//        }
//    }

    val actionSheet = rememberActionSheetState()
    fun showPostOptions() {
        actionSheet.show(
            options = listOf(
                ActionSheetItem(
                    labelRes = R.string.moments_post_camera,
                    descriptionRes = R.string.moments_post_camera_desc
                ),
                ActionSheetItem(
                    labelRes = R.string.moments_post_gallery,
                )
            )
        ) { index ->
            when (index) {
                0 -> {}
                1 -> onNavigateToPost()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.surface)
    ) {
        AnimatedVisibility(
            visible = !coverExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.zIndex(1f)
        ) {
            TopBar(
                listState = listState,
                onBack = onBack,
                onPost = ::showPostOptions
            )
        }

        LazyColumn(state = listState) {
            item(contentType = "moments_hero") {
                MomentsHero(
                    cover = state.coverFor(viewModel.profile.id),
                    profile = viewModel.profile,
                    expanded = coverExpanded,
                    onCoverClick = { coverExpanded = !coverExpanded },
                    onChangeCover = onNavigateToCover,
                    onProfileClick = {}
                )
                Spacer(Modifier.height(48.dp))
            }
            items(
                items = state.moments,
                key = { it.id },
                contentType = { "moment_item" }
            ) { moment ->
                MomentItem(
                    moment = moment,
                    myId = viewModel.profile.id,
                    onLike = { viewModel.toggleLike(moment.id) },
                    onComment = { },
                    onDelete = { },
                    onVideoClick = {},
                    onImageClick = { index -> }
                )
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun TopBar(
    listState: LazyListState,
    onBack: () -> Unit,
    onPost: () -> Unit
) {
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
    val isReached = topBarProgress >= 0.55f
    val topBarBackground = WeTheme.colorScheme.background.copy(alpha = topBarProgress)

    WeTopAppBar(
        title = if (isReached) "朋友圈" else null,
        onBack = onBack,
        containerColor = topBarBackground,
        contentColor = if (isReached) TextPrimaryLight else TextPrimaryDark
    ) {
        IconButton(
            icon = if (isReached) R.drawable.ic_camera_outlined else R.drawable.ic_camera_filled_1,
            description = "发朋友圈",
            onClick = onPost
        )
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
    val resources = LocalResources.current
    var actionsExpanded by remember(moment.id) { mutableStateOf(false) }
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    var buttonSize by remember { mutableStateOf(Size.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        AsyncImage(
            model = moment.authorAvatar,
            contentDescription = null,
            error = painterResource(R.drawable.img_avatar_placeholder),
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = moment.authorName,
                color = WeTheme.colorScheme.link,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (moment.content.isNotBlank()) {
                Text(
                    text = moment.content,
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            if (moment.images.isNotEmpty()) {
                MomentImages(moment.id, moment.images, onImageClick)
            }
            moment.video?.let { video ->
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(width = 210.dp, height = 280.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black)
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
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color(0x66000000))
                            .padding(6.dp, 3.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = moment.createdAt.toRelativeDateTime(resources),
                    color = WeTheme.colorScheme.textTertiary,
                    fontSize = 12.sp
                )
                if (moment.authorId == myId) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_filled),
                        contentDescription = null,
                        tint = WeTheme.colorScheme.link,
                        modifier = Modifier
                            .size(17.dp)
                            .onTap(
                                role = Role.Button,
                                onClickLabel = "删除",
                                onClick = onDelete
                            )
                    )
                }
                Spacer(Modifier.weight(1f))

                Popup(
                    offset = buttonPosition.run {
                        IntOffset(
                            (x - buttonSize.width - 460).toInt(),
                            (y - buttonSize.height / 2).toInt(),
                        )
                    },
                    onDismissRequest = { actionsExpanded = false }
                ) {
                    AnimatedVisibility(
                        visible = actionsExpanded,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF4C5154)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        actionsExpanded = false
                                        onLike()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_like_outlined),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (moment.likes.any { it.userId == myId }) "取消" else "赞",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(18.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFF383D40)
                            )
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        actionsExpanded = false
                                        onComment()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_message_outlined),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "评论",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(WeTheme.colorScheme.surfaceVariant)
                        .onGloballyPositioned { coordinates ->
                            buttonPosition = coordinates.positionInParent()
                            buttonSize = Size(
                                width = coordinates.size.width.toFloat(),
                                height = coordinates.size.height.toFloat()
                            )
                        }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "赞和评论",
                            onClick = { actionsExpanded = !actionsExpanded }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_filled_1),
                        contentDescription = null,
                        tint = WeTheme.colorScheme.link,
                        modifier = Modifier.width(22.dp)
                    )
                }
            }
            if (moment.likes.isNotEmpty() || moment.comments.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Canvas(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(12.dp, 6.dp)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(0f, size.height)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path, Color(0xFFF3F3F5))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F3F5), RoundedCornerShape(2.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        if (moment.likes.isNotEmpty()) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.ic_like_outlined),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(14.dp),
                                    tint = Color(0xFF576B95)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    moment.likes.joinToString("，") { it.userName },
                                    color = Color(0xFF576B95),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        if (moment.likes.isNotEmpty() && moment.comments.isNotEmpty()) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                        }
                        moment.comments.forEach {
                            Text(
                                buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFF576B95),
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(it.userName)
                                    }
                                    append("：${it.content}")
                                },
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
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
    val columns = if (shown.size == 4) 2 else 3
    val imageSize = if (shown.size == 1) 180.dp else if (shown.size == 4) 110.dp else 90.dp

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        shown.withIndex().chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { indexedPath ->
                    val index = indexedPath.index
                    val path = indexedPath.value
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        modifier = Modifier
                            .momentsMediaSharedElement(momentId, index)
                            .size(imageSize)
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
