package top.chengdongqing.wechat.features.call.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.call.domain.model.CallActions
import top.chengdongqing.wechat.features.call.domain.model.CallState
import top.chengdongqing.wechat.features.call.domain.model.CallUiState

@Composable
fun CallControlBar(
    state: CallUiState,
    actions: CallActions,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        if (state.isVideoCall) {
            VideoCallControls(state, actions)
        } else {
            VoiceCallControls(state, actions)
        }
    }
}

@Composable
private fun VideoCallControls(
    state: CallUiState,
    actions: CallActions,
) {
    when (state.callState) {
        CallState.Incoming -> {
            ControlRow {
                CameraSwitchToggle(actions, showLabel = true)
                SpeakerToggle(state, actions)
                VideoToggle(state, actions)
            }
            ControlRow {
                RejectButton(actions)
                Spacer(modifier = Modifier.size(68.dp))
                AcceptButton(actions)
            }
        }

        CallState.Ended ->
            ControlRow {
                HangupButton(actions, backgroundColor = Color.Gray)
            }

        else -> {
            ControlRow {
                MicToggle(state, actions)
                SpeakerToggle(state, actions)
                VideoToggle(state, actions)
            }
            ControlRow {
                Spacer(modifier = Modifier.size(68.dp))
                HangupButton(actions)
                CameraSwitchToggle(actions, showLabel = false, showBackground = false)
            }
        }
    }
}

@Composable
private fun VoiceCallControls(
    state: CallUiState,
    actions: CallActions,
) {
    when (state.callState) {
        CallState.Incoming -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                RejectButton(actions, showLabel = true)
                AcceptButton(actions, showLabel = true)
            }
        }

        CallState.Ended ->
            ControlRow {
                HangupButton(actions, backgroundColor = Color.Gray)
            }

        else ->
            ControlRow {
                MicToggle(state, actions)
                HangupButton(actions, showLabel = true)
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
private fun MicToggle(state: CallUiState, actions: CallActions) = ControlToggle(
    icon = if (state.isMicOn) R.drawable.ic_mic_filled else R.drawable.ic_mic_off_filled,
    label = state.isMicOn.toStatusText("麦克风"),
    isActive = state.isMicOn,
    onClick = actions.onToggleMic
)

@Composable
private fun SpeakerToggle(state: CallUiState, actions: CallActions) = ControlToggle(
    icon = if (state.isSpeakerOn) R.drawable.ic_speaker_filled else R.drawable.ic_speaker_off_filled,
    label = state.isSpeakerOn.toStatusText("扬声器"),
    isActive = state.isSpeakerOn,
    onClick = actions.onToggleSpeaker
)

@Composable
private fun VideoToggle(state: CallUiState, actions: CallActions) = ControlToggle(
    icon = if (state.isVideoOn) R.drawable.ic_video_filled else R.drawable.ic_video_off_filled,
    label = state.isVideoOn.toStatusText("摄像头"),
    isActive = state.isVideoOn,
    onClick = actions.onToggleVideo
)

@Composable
private fun CameraSwitchToggle(
    actions: CallActions,
    showLabel: Boolean,
    showBackground: Boolean = true
) = ControlToggle(
    icon = R.drawable.ic_camera_switch_filled,
    label = if (showLabel) "翻转" else null,
    backgroundColor = if (showBackground) Color.White.copy(alpha = 0.2f) else Color.Unspecified,
    onClick = actions.onSwitchCamera
)

@Composable
private fun HangupButton(
    actions: CallActions,
    backgroundColor: Color = Danger,
    showLabel: Boolean = false,
) =
    ControlToggle(
        icon = R.drawable.ic_hangup_filled,
        label = if (showLabel) "挂断" else null,
        backgroundColor = backgroundColor,
        onClick = actions.onHangup
    )

@Composable
private fun RejectButton(actions: CallActions, showLabel: Boolean = false) =
    ControlToggle(
        icon = R.drawable.ic_hangup_filled,
        label = if (showLabel) "拒绝" else null,
        backgroundColor = Danger,
        onClick = actions.onReject
    )

@Composable
private fun AcceptButton(actions: CallActions, showLabel: Boolean = false) = ControlToggle(
    icon = R.drawable.ic_call_filled,
    label = if (showLabel) "接听" else null,
    backgroundColor = WeTheme.colorScheme.primary,
    onClick = actions.onAccept
)

private fun Boolean.toStatusText(label: String) = label + if (this) "已开" else "已关"