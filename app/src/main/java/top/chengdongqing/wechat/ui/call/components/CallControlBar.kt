package top.chengdongqing.wechat.ui.call.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.call.model.CallState
import top.chengdongqing.wechat.data.call.model.CallType
import top.chengdongqing.wechat.ui.call.CallUiState
import top.chengdongqing.wechat.ui.util.weClickable

/**
 * 通话控制栏
 *
 * 根据通话状态和类型显示不同的控制按钮
 */
@Composable
fun CallControlBar(
    state: CallUiState,
    modifier: Modifier = Modifier,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSwitchCamera: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (state.callType == CallType.Video) 30.dp else 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.callState) {
            // 来电响铃状态
            is CallState.Ringing -> {
                IncomingCallControls(
                    callType = state.callType,
                    onAccept = onAcceptCall,
                    onReject = onRejectCall
                )
            }

            // 通话中或连接中状态
            is CallState.Active, is CallState.Connecting -> {
                ActiveCallControls(
                    state = state,
                    onToggleMic = onToggleMic,
                    onToggleSpeaker = onToggleSpeaker,
                    onSwitchCamera = onSwitchCamera,
                    onHangup = onRejectCall
                )
            }

            // 其他状态（已结束等）
            else -> {
                EndedCallControls(
                    onClose = onRejectCall
                )
            }
        }
    }
}

/**
 * 来电控制按钮
 */
@Composable
private fun IncomingCallControls(
    callType: CallType,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 拒接按钮
        CircularControlButton(
            iconResId = R.drawable.ic_hangup_filled,
            text = "挂断",
            backgroundColor = Color(0xFFFF4D4F),
            onClick = onReject
        )

        // 接听按钮
        CircularControlButton(
            iconResId = when (callType) {
                CallType.Voice -> R.drawable.ic_voice_call_filled
                CallType.Video -> R.drawable.ic_video_call_filled
            },
            text = "接听",
            backgroundColor = Color(0xFF52C41A),
            onClick = onAccept
        )
    }
}

/**
 * 通话中控制按钮
 */
@Composable
private fun RowScope.ActiveCallControls(
    state: CallUiState,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSwitchCamera: () -> Unit,
    onHangup: () -> Unit
) {
    // 麦克风按钮
    CircularControlButton(
        iconResId = if (state.audioConfig.isMicEnabled) {
            R.drawable.ic_mic_filled
        } else {
            R.drawable.ic_mic_off_filled
        },
        text = "麦克风",
        onClick = onToggleMic,
        isActive = state.audioConfig.isMicEnabled
    )

    // 视频模式下显示切换摄像头按钮
    if (state.callType == CallType.Video && state.isCallActive) {
        SwitchCameraButton(onClick = onSwitchCamera)
    }

    // 挂断按钮
    CircularControlButton(
        iconResId = R.drawable.ic_hangup_filled,
        text = "挂断",
        backgroundColor = Color(0xFFFF4D4F),
        onClick = onHangup
    )

    // 扬声器按钮
    CircularControlButton(
        iconResId = if (state.audioConfig.isSpeakerEnabled) {
            R.drawable.ic_speaker_filled
        } else {
            R.drawable.ic_speaker_off_filled
        },
        text = "扬声器",
        onClick = onToggleSpeaker,
        isActive = state.audioConfig.isSpeakerEnabled
    )
}

/**
 * 通话结束控制按钮
 */
@Composable
private fun EndedCallControls(
    onClose: () -> Unit
) {
    CircularControlButton(
        iconResId = R.drawable.ic_hangup_filled,
        text = "关闭",
        backgroundColor = Color.Gray,
        onClick = onClose
    )
}

/**
 * 切换摄像头按钮（特殊样式）
 */
@Composable
private fun SwitchCameraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Filled.FlipCameraAndroid,
        contentDescription = "切换摄像头",
        tint = Color.White,
        modifier = modifier
            .size(32.dp)
            .weClickable(onClick)
    )
}