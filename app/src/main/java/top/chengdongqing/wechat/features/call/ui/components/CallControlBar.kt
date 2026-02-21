package top.chengdongqing.wechat.features.call.ui.components

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.call.domain.model.CallActions
import top.chengdongqing.wechat.features.call.domain.model.CallState
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.call.domain.model.CallUiState

/**
 * 通话底部控制栏
 *
 * 根据通话类型（视频/语音）和通话状态（来电/去电/通话中/结束）渲染对应的操作按钮。
 */
@Composable
fun CallControlBar(state: CallUiState, actions: CallActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        if (state.isVideoCall) VideoCallControls(state, actions)
        else VoiceCallControls(state, actions)
    }
}

/**
 * 视频通话控制区
 *
 * Incoming：翻转/扬声器/摄像头 + 拒绝/接听
 * Ended：灰色挂断按钮（不可操作，仅展示结束状态）
 * 其他：麦克风/扬声器/摄像头 + 挂断或取消
 */
@Composable
private fun VideoCallControls(state: CallUiState, actions: CallActions) {
    when (state.callState) {
        CallState.Incoming -> {
            ControlRow {
                CameraSwitchToggle(actions, showLabel = true, enabled = state.isVideoOn)
                SpeakerToggle(state, actions)
                VideoToggle(state, actions)
            }
            ControlRow {
                DeclineButton(actions)
                Spacer(Modifier.size(72.dp))
                AcceptButton(type = CallType.Video, actions = actions)
            }
        }

        CallState.Ended -> ControlRow {
            HangupButton(actions, backgroundColor = Color.Gray)
        }

        else -> {
            ControlRow {
                MicToggle(state, actions)
                SpeakerToggle(state, actions)
                VideoToggle(state, actions)
            }
            ControlRow {
                Spacer(Modifier.size(72.dp))
                if (state.isCallActive) HangupButton(actions) else CancelButton(actions)
                if (state.isVideoOn) CameraSwitchToggle(
                    actions,
                    showLabel = false,
                    showBackground = false
                )
                else Spacer(Modifier.size(72.dp))
            }
        }
    }
}

/**
 * 语音通话控制区
 *
 * Incoming：拒绝 + 接听（带标签）
 * Ended：灰色挂断按钮
 * 其他：麦克风 + 挂断或取消 + 扬声器
 */
@Composable
private fun VoiceCallControls(state: CallUiState, actions: CallActions) {
    when (state.callState) {
        CallState.Incoming -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            DeclineButton(actions, showLabel = true)
            AcceptButton(type = CallType.Voice, actions = actions, showLabel = true)
        }

        CallState.Ended -> ControlRow {
            HangupButton(actions, backgroundColor = Color.Gray)
        }

        else -> ControlRow {
            MicToggle(state, actions)
            if (state.isCallActive) HangupButton(actions, showLabel = true)
            else CancelButton(actions, showLabel = true)
            SpeakerToggle(state, actions)
        }
    }
}

@Composable
private fun ControlRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// ==================== 控制按钮 ====================

@Composable
private fun MicToggle(state: CallUiState, actions: CallActions) = ControlToggle(
    icon = if (state.isMicOn) R.drawable.ic_mic_filled else R.drawable.ic_mic_off_filled,
    label = state.isMicOn.toStatusLabel("麦克风"),
    onClick = actions.onToggleMic,
    isActive = state.isMicOn
)

@Composable
private fun SpeakerToggle(state: CallUiState, actions: CallActions) = ControlToggle(
    icon = if (state.isSpeakerOn) R.drawable.ic_speaker_filled else R.drawable.ic_speaker_off_filled,
    label = state.isSpeakerOn.toStatusLabel("扬声器"),
    onClick = actions.onToggleSpeaker,
    isActive = state.isSpeakerOn
)

/**
 * 摄像头开关按钮
 *
 * 无摄像头权限时自动关闭视频；点击时若无权限则请求授权，授权后再切换。
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun VideoToggle(state: CallUiState, actions: CallActions) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) actions.onToggleVideo()
    }

    // 无摄像头权限时自动关闭视频（来电时权限尚未授予的场景）
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted && state.isVideoOn) {
            actions.onToggleVideo()
        }
    }

    ControlToggle(
        icon = if (state.isVideoOn) R.drawable.ic_video_filled else R.drawable.ic_video_off_filled,
        label = state.isVideoOn.toStatusLabel("摄像头"),
        onClick = {
            if (cameraPermission.status.isGranted) actions.onToggleVideo()
            else cameraPermission.launchPermissionRequest()
        },
        isActive = state.isVideoOn
    )
}

@Composable
private fun CameraSwitchToggle(
    actions: CallActions,
    showLabel: Boolean,
    enabled: Boolean = true,
    showBackground: Boolean = true
) = ControlToggle(
    icon = R.drawable.ic_camera_switch_filled,
    label = if (showLabel) "翻转" else null,
    enabled = enabled,
    backgroundColor = if (showBackground) Color.White.copy(alpha = 0.2f) else Color.Unspecified,
    onClick = actions.onSwitchCamera
)

@Composable
private fun CancelButton(actions: CallActions, showLabel: Boolean = false) = ControlToggle(
    icon = R.drawable.ic_hangup_filled,
    label = if (showLabel) "取消" else null,
    onClick = actions.onCancel,
    backgroundColor = Danger
)

@Composable
private fun HangupButton(
    actions: CallActions,
    backgroundColor: Color = Danger,
    showLabel: Boolean = false
) = ControlToggle(
    icon = R.drawable.ic_hangup_filled,
    label = if (showLabel) "挂断" else null,
    onClick = actions.onHangup,
    backgroundColor = backgroundColor
)

@Composable
private fun DeclineButton(actions: CallActions, showLabel: Boolean = false) = ControlToggle(
    icon = R.drawable.ic_hangup_filled,
    label = if (showLabel) "拒绝" else null,
    onClick = actions.onDecline,
    backgroundColor = Danger
)

/**
 * 接听按钮
 *
 * 权限未授予时先请求；获得全部权限后自动接听；权限被拒绝则自动挂断。
 * 视频通话需要 CAMERA + RECORD_AUDIO，语音通话只需 RECORD_AUDIO。
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun AcceptButton(type: CallType, actions: CallActions, showLabel: Boolean = false) {
    val requiredPermissions = remember(type) {
        buildList {
            if (type == CallType.Video) {
                add(Manifest.permission.CAMERA)
            }
            add(Manifest.permission.RECORD_AUDIO)
        }
    }
    val permissionsState = rememberMultiplePermissionsState(requiredPermissions)
    var isPendingAccept by remember { mutableStateOf(false) }

    // 获得全部权限后自动接听
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (isPendingAccept && permissionsState.allPermissionsGranted) {
            actions.onAccept()
        }
    }

    // 权限被拒绝，自动挂断
    LaunchedEffect(permissionsState.shouldShowRationale) {
        if (isPendingAccept && !permissionsState.allPermissionsGranted) {
            actions.onDecline()
        }
    }

    ControlToggle(
        icon = R.drawable.ic_call_filled,
        label = if (showLabel) "接听" else null,
        onClick = {
            if (permissionsState.allPermissionsGranted) {
                actions.onAccept()
            } else {
                isPendingAccept = true
                permissionsState.launchMultiplePermissionRequest()
            }
        },
        backgroundColor = WeTheme.colorScheme.primary
    )
}

// ==================== 工具 ====================

/** 将开关状态转为带后缀的标签，如"麦克风已开"/"麦克风已关" */
private fun Boolean.toStatusLabel(label: String) = "$label${if (this) "已开" else "已关"}"