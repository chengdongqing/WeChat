package top.chengdongqing.wechat.feature.moments.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryDark
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryLight
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.moments.model.coverFor

@Composable
fun MomentsScreen(
    onBack: () -> Unit,
    onNavigateToPost: () -> Unit,
    onNavigateToCover: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var coverExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

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
            .then(
                // 当前封面展开时，滑动页面自动触发收起封面
                if (coverExpanded) {
                    Modifier.pointerInput(Unit) {
                        var accumulated = 0f
                        detectVerticalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                // 只关心下滑,上滑不响应
                                accumulated = (accumulated + dragAmount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                coverExpanded = false
                            }
                        )
                    }
                } else Modifier
            )
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

        LazyColumn(
            state = listState,
            userScrollEnabled = !coverExpanded
        ) {
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
                    onImageClick = { _ -> }
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
