package top.chengdongqing.wechat.core.common.camera

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import top.chengdongqing.wechat.core.common.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import kotlin.math.abs
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
internal fun BoxScope.ControlBar(state: CameraState) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TipText(state)

        if (!state.isUsingFrontCamera) {
            ZoomControlBar(state)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = !state.isUsingFrontCamera,
                onClick = { state.toggleFlash() },
                modifier = Modifier.alpha(if (state.isUsingFrontCamera) 0f else 1f)
            ) {
                Icon(
                    imageVector = if (state.isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = stringResource(DesignR.string.camera_flash_toggle),
                    tint = Color.White
                )
            }
            CaptureButton(state)
            IconButton(
                onClick = { state.switchCamera() }
            ) {
                Icon(
                    Icons.Filled.FlipCameraAndroid,
                    contentDescription = stringResource(DesignR.string.camera_switch),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ZoomControlBar(state: CameraState) {
    val haptic = LocalHapticFeedback.current
    val zoomOptions = state.availableZoomSteps
    // 当前最接近的索引
    val selectedIndex = remember(state.currentZoom, zoomOptions) {
        zoomOptions.indexOfFirst { abs(it - state.currentZoom) < 0.1f }.coerceAtLeast(0)
    }

    // 每个按钮宽度是 30dp + 间距 4dp
    val itemWidth = 30.dp
    val spacing = 4.dp
    // 指示器的偏移量
    val indicatorOffset by animateDpAsState(
        targetValue = (itemWidth + spacing) * selectedIndex,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .offset(y = 20.dp)
            .wrapContentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(4.dp)
    ) {
        // 在底层滑动的背景圆圈
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(x = indicatorOffset.roundToPx(), y = 0)
                }
                .size(itemWidth)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // 按钮组
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomOptions.forEachIndexed { index, step ->
                val isSelected = index == selectedIndex

                Box(
                    modifier = Modifier
                        .size(itemWidth)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // 执行变焦
                            state.setZoom(step)
                            // 振动反馈
                            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val label = if (step < 1f) "%.1f" else "${step.toInt()}"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label.format(step),
                            color = animateColorAsState(
                                if (isSelected) WeTheme.colorScheme.primary else Color.White
                            ).value,
                            fontSize = 12.sp
                        )
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Text(
                                text = "x",
                                color = WeTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TipText(state: CameraState) {
    val resources = LocalResources.current
    val tips = remember {
        buildList {
            if (state.type == VisualMediaType.ImageAndVideo || state.type == VisualMediaType.Image) {
                add(resources.getString(DesignR.string.camera_tip_photo))
            }
            if (state.type == VisualMediaType.ImageAndVideo || state.type == VisualMediaType.Video) {
                add(resources.getString(DesignR.string.camera_tip_video))
            }
        }.joinToString(resources.getString(DesignR.string.separator))
    }

    Text(
        text = tips,
        color = Color.White,
        fontSize = 14.sp
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CaptureButton(state: CameraState) {
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (state.type != VisualMediaType.Image) {
                            if (audioPermissionState.status.isGranted) {
                                state.startRecording()
                            } else {
                                audioPermissionState.launchPermissionRequest()
                            }
                        }
                    },
                    onPress = {
                        awaitRelease()
                        if (state.isRecording) {
                            state.stopRecording()
                        }
                    }
                ) {
                    if (state.type != VisualMediaType.Video) {
                        state.takePhoto()
                    }
                }
            }
    ) {
        if (!state.isRecording) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .background(Color.White, CircleShape)
            )
        } else {
            val borderColor = Color.LightGray
            val activeColor = WeTheme.colorScheme.primary

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        val strokeWidth = 6.dp.toPx()
                        drawCircle(
                            color = borderColor,
                            radius = size.width / 2,
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = activeColor,
                            startAngle = -90f,
                            sweepAngle = 360 * state.videoProgress,
                            useCenter = false,
                            style = Stroke(strokeWidth)
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}