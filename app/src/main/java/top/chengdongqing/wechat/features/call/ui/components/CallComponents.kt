package top.chengdongqing.wechat.features.call.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.features.call.data.CallActions
import top.chengdongqing.wechat.features.call.data.CallUiState

@Composable
fun CallBackground(avatarResId: Int, isVideoMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isVideoMode) Color.Black else Color(0xFF2C2C2C))
    ) {
        Image(
            painter = painterResource(avatarResId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isVideoMode) 60.dp else 40.dp),
            contentScale = ContentScale.Crop,
            alpha = if (isVideoMode) 0.3f else 0.4f
        )
    }
}

@Composable
fun CallUserInfo(userName: String, statusText: String?, showAvatar: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showAvatar) {
            Image(
                painter = painterResource(R.drawable.img_avatar),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(userName, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        statusText?.let {
            Spacer(modifier = Modifier.height(40.dp))
            Text(it, fontSize = 14.sp, color = Color.White.copy(0.7f))
        }
    }
}

/**
 * 通话控制栏
 *
 * ★ 修复: 根据状态区分挂断/拒绝
 * - 来电中 (isRinging): 显示 接听 + 拒绝，拒绝调 onReject
 * - 其他状态: 显示 功能开关 + 挂断，挂断调 onHangup
 */
@Composable
fun CallControlBar(
    state: CallUiState,
    actions: CallActions,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        if (state.isRinging) {
            // ========== 来电界面: 接听 + 拒绝 ==========
            Row(Modifier.fillMaxWidth(), Arrangement.Center) {
                ControlToggle(
                    icon = R.drawable.ic_call_filled,
                    label = "接听",
                    backgroundColor = WeTheme.colorScheme.primary,
                    onClick = actions.onAccept
                )
                Spacer(Modifier.width(60.dp))
                ControlToggle(
                    icon = R.drawable.ic_hangup_filled,
                    label = "拒绝",
                    backgroundColor = Danger,
                    onClick = actions.onReject   // ★ 来电拒绝
                )
            }
        } else {
            // ========== 通话中/呼出中: 功能开关 + 挂断 ==========

            // 功能开关行
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                ControlToggle(
                    icon = if (state.isMicOn) R.drawable.ic_mic_filled else R.drawable.ic_mic_off_filled,
                    label = "麦克风",
                    isActive = state.isMicOn,
                    onClick = actions.onToggleMic
                )
                ControlToggle(
                    icon = if (state.isSpeakerOn) R.drawable.ic_speaker_filled else R.drawable.ic_speaker_off_filled,
                    label = "扬声器",
                    isActive = state.isSpeakerOn,
                    onClick = actions.onToggleSpeaker
                )
                if (state.isVideoCall) {
                    ControlToggle(
                        icon = R.drawable.ic_camera_switch_filled,
                        label = "翻转",
                        onClick = actions.onSwitchCamera
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 挂断按钮
            Row(Modifier.fillMaxWidth(), Arrangement.Center) {
                ControlToggle(
                    icon = R.drawable.ic_hangup_filled,
                    label = "挂断",
                    backgroundColor = Danger,
                    onClick = actions.onHangup   // ★ 通话中挂断
                )
            }
        }
    }
}

/**
 * 统一的通话控制按钮
 * @param isActive 为 null 时表示普通动作按钮（如挂断），为 true/false 时表示状态开关（如麦克风）
 */
@Composable
fun ControlToggle(
    @DrawableRes icon: Int,
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean? = null,
    backgroundColor: Color = Color.White.copy(alpha = 0.2f),
    activeColor: Color = Color.White,
    iconTint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    when (isActive) {
                        true -> activeColor
                        else -> backgroundColor
                    }
                )
                .weClickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                // 如果是激活状态，图标颜色通常反转为黑色
                tint = if (isActive == true) Color.Black else iconTint
            )
        }

        label?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color.White, fontSize = 12.sp)
        }
    }
}