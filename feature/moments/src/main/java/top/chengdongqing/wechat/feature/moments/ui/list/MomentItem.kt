package top.chengdongqing.wechat.feature.moments.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.common.time.toRelativeDateTime
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.DividerDark
import top.chengdongqing.wechat.core.designsystem.theme.Gray
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.moments.model.Moment
import top.chengdongqing.wechat.feature.moments.model.MomentComment
import top.chengdongqing.wechat.feature.moments.model.MomentVideo

@Composable
internal fun MomentItem(
    moment: Moment,
    myId: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onDelete: () -> Unit,
    onVideoClick: () -> Unit,
    onImageClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Avatar(moment)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            AuthorAndContent(moment)

            when {
                moment.images.isNotEmpty() -> {
                    MomentImageGroup(moment.id, moment.images, onImageClick)
                }

                moment.video != null -> {
                    MomentVideoThumbnail(video = moment.video, onClick = onVideoClick)
                }
            }

            MomentFooter(
                moment = moment,
                myId = myId,
                onLike = onLike,
                onComment = onComment,
                onDelete = onDelete
            )

            if (moment.likes.isNotEmpty() || moment.comments.isNotEmpty()) {
                MomentLikesAndComments(moment = moment)
            }
        }
    }
}

@Composable
private fun AuthorAndContent(moment: Moment) {
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
}

@Composable
private fun Avatar(moment: Moment) {
    AsyncImage(
        model = moment.authorAvatar,
        contentDescription = null,
        error = painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(4.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun MomentVideoThumbnail(video: MomentVideo, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .size(width = 210.dp, height = 280.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Black)
            .clickable(onClick = onClick),
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
            text = formatDuration(video.duration),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color(0x66000000))
                .padding(6.dp, 3.dp)
        )
    }
}

@Composable
private fun MomentFooter(
    moment: Moment,
    myId: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onDelete: () -> Unit
) {
    var actionsExpanded by remember(moment.id) { mutableStateOf(false) }
    var buttonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val resources = LocalResources.current
    val density = LocalDensity.current

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
                        onClickLabel = stringResource(R.string.action_delete),
                        onClick = onDelete
                    )
            )
        }
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WeTheme.colorScheme.surfaceVariant)
                .onGloballyPositioned { buttonCoordinates = it }
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.moment_action_comment),
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

        val visibilityState = remember {
            MutableTransitionState(false)
        }
        LaunchedEffect(actionsExpanded) {
            visibilityState.targetState = actionsExpanded
        }

        if (visibilityState.currentState || visibilityState.targetState) {
            Popup(
                popupPositionProvider = remember(buttonCoordinates) {
                    val gapPx = with(density) { 6.dp.roundToPx() }
                    MomentActionPopupPositionProvider(buttonCoordinates, gapPx)
                },
                properties = PopupProperties(
                    focusable = true,
                    usePlatformDefaultWidth = false
                ),
                onDismissRequest = { actionsExpanded = false }
            ) {
                AnimatedVisibility(
                    visibleState = visibilityState,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    MomentActionsPopup(
                        isLiked = remember(moment.likes, myId) {
                            moment.likes.any { it.userId == myId }
                        },
                        onLike = {
                            actionsExpanded = false
                            onLike()
                        },
                        onComment = {
                            actionsExpanded = false
                            onComment()
                        }
                    )
                }
            }
        }
    }
}

private class MomentActionPopupPositionProvider(
    private val buttonCoordinates: LayoutCoordinates?,
    private val gapPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val buttonRect = buttonCoordinates?.boundsInWindow()
            ?: return IntOffset.Zero

        // 气泡右边缘紧贴按钮左边缘
        val x = (buttonRect.left - popupContentSize.width - gapPx)
            .toInt()
            .coerceAtLeast(0)

        // 垂直方向与按钮居中对齐
        val y =
            (buttonRect.top + (buttonRect.height - popupContentSize.height) / 2)
                .toInt()
                .coerceIn(
                    0,
                    (windowSize.height - popupContentSize.height).coerceAtLeast(
                        0
                    )
                )

        return IntOffset(x, y)
    }
}

@Composable
private fun MomentActionsPopup(
    isLiked: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Gray),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PopupActionItem(
            iconRes = R.drawable.ic_like_outlined,
            label = if (isLiked) {
                stringResource(R.string.moment_action_unlike)
            } else {
                stringResource(R.string.moment_action_like)
            },
            onClick = onLike
        )
        VerticalDivider(
            modifier = Modifier.height(18.dp),
            thickness = 0.5.dp,
            color = DividerDark
        )
        PopupActionItem(
            iconRes = R.drawable.ic_message_outlined,
            label = stringResource(R.string.moment_action_comment),
            onClick = onComment
        )
    }
}

@Composable
private fun PopupActionItem(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun MomentLikesAndComments(moment: Moment) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Canvas(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(12.dp, 6.dp)
        ) {
            val path = Path().apply {
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
                val likesText = remember(moment.likes) {
                    moment.likes.joinToString("，") { it.userName }
                }
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
                        likesText,
                        color = Color(0xFF576B95),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                }
            }
            if (moment.likes.isNotEmpty() && moment.comments.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.3f)
                )
            }
            moment.comments.forEach { comment ->
                key(comment.id) {
                    CommentLine(comment)
                }
            }
        }
    }
}

@Composable
private fun CommentLine(comment: MomentComment) {
    val annotated = remember(comment.id, comment.userName, comment.content) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFF576B95), fontWeight = FontWeight.Bold)) {
                append(comment.userName)
            }
            append("：${comment.content}")
        }
    }
    Text(
        annotated,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}

@OptIn(ExperimentalGridApi::class)
@Composable
private fun MomentImageGroup(
    momentId: String,
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    val shown = images.take(9)
    val columns = if (shown.size == 4) 2 else 3
    val imageSize = when (shown.size) {
        1 -> 180.dp
        4 -> 110.dp
        else -> 90.dp
    }
    val gap = 4.dp
    val rows = (shown.size + columns - 1) / columns

    Grid(
        modifier = Modifier.padding(top = 8.dp),
        config = {
            repeat(columns) { column(imageSize) }
            repeat(rows) { row(imageSize) }
            columnGap(gap)
            rowGap(gap)
        }
    ) {
        shown.forEachIndexed { index, path ->
            AsyncImage(
                model = path,
                contentDescription = null,
                modifier = Modifier
                    .momentsMediaSharedElement(momentId, index)
                    .clip(RoundedCornerShape(2.dp))
                    .clickable { onImageClick(index) },
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun formatDuration(duration: Long): String =
    "%d:%02d".format(duration / 60_000, duration / 1_000 % 60)
