package top.chengdongqing.wechat.feature.call.ui

import android.Manifest
import android.content.Context
import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.SemanticError
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.CallState
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.feature.call.domain.model.CallActions
import top.chengdongqing.wechat.feature.call.domain.model.CallUiState
import top.chengdongqing.wechat.feature.call.R as CallR

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

@Composable
private fun MicToggle(state: CallUiState, actions: CallActions) {
    val context = LocalContext.current
    ControlToggle(
        icon = if (state.isMicOn) R.drawable.ic_mic_filled else R.drawable.ic_mic_off_filled,
        label = state.isMicOn.toStatusLabel(context, CallR.string.call_control_mic),
        onClick = actions.onToggleMic,
        isActive = state.isMicOn
    )
}

@Composable
private fun SpeakerToggle(state: CallUiState, actions: CallActions) {
    val context = LocalContext.current
    ControlToggle(
        icon = if (state.isSpeakerOn) R.drawable.ic_speaker_filled else R.drawable.ic_speaker_off_filled,
        label = state.isSpeakerOn.toStatusLabel(context, CallR.string.call_control_speaker),
        onClick = actions.onToggleSpeaker,
        isActive = state.isSpeakerOn
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun VideoToggle(state: CallUiState, actions: CallActions) {
    val context = LocalContext.current
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
        label = state.isVideoOn.toStatusLabel(context, CallR.string.call_control_camera),
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
    label = if (showLabel) stringResource(CallR.string.call_control_flip) else null,
    enabled = enabled,
    backgroundColor = if (showBackground) Color.White.copy(alpha = 0.2f) else Color.Unspecified,
    onClick = actions.onSwitchCamera
)

@Composable
private fun CancelButton(actions: CallActions, showLabel: Boolean = false) = ControlToggle(
    icon = R.drawable.ic_hangup_filled,
    label = if (showLabel) stringResource(CallR.string.call_control_cancel) else null,
    onClick = actions.onCancel,
    backgroundColor = SemanticError
)

@Composable
private fun HangupButton(
    actions: CallActions,
    backgroundColor: Color = SemanticError,
    showLabel: Boolean = false
) = ControlToggle(
    icon = R.drawable.ic_hangup_filled,
    label = if (showLabel) stringResource(CallR.string.call_control_hangup) else null,
    onClick = actions.onHangup,
    backgroundColor = backgroundColor
)

@Composable
private fun DeclineButton(actions: CallActions, showLabel: Boolean = false) = ControlToggle(
    icon = R.drawable.ic_hangup_filled,
    label = if (showLabel) stringResource(CallR.string.call_control_decline) else null,
    onClick = actions.onDecline,
    backgroundColor = SemanticError
)

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
        label = if (showLabel) stringResource(CallR.string.call_control_accept) else null,
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

/**
 * 将开关状态转为带后缀的标签，如"麦克风已开"/"麦克风已关"
 */
private fun Boolean.toStatusLabel(context: Context, @StringRes labelRes: Int): String {
    val label = context.getString(labelRes)
    val template = if (this) CallR.string.call_control_status_on else CallR.string.call_control_status_off
    return context.getString(template, label)
}
