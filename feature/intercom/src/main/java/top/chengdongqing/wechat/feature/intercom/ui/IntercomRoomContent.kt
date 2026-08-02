package top.chengdongqing.wechat.feature.intercom.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.intercom.model.IntercomMember

@Composable
internal fun SpeakingStage(
    isTalking: Boolean,
    speakers: List<IntercomMember>
) {
    val remoteSpeakers = speakers.filterNot(IntercomMember::isMe)
    val primarySpeaker = speakers.firstOrNull()
    val isActive = isTalking || speakers.isNotEmpty()
    val pulse by rememberInfiniteTransition(label = "speakerPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(148.dp)
                    .graphicsLayer {
                        scaleX = if (isActive) pulse else 1f; scaleY = if (isActive) pulse else 1f
                    }
                    .background(
                        Color(0xFF07C160).copy(alpha = if (isActive) .18f else .08f),
                        CircleShape
                    )
            )
            Box(
                Modifier
                    .size(116.dp)
                    .background(Color(0xFF183D2A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isTalking) "我" else primarySpeaker?.nickname?.take(1) ?: "—",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(42.dp)
                    .background(if (isActive) Color(0xFF07C160) else Color(0xFF53665B), CircleShape)
                    .border(4.dp, Color(0xFF0D1D15), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_mic_filled),
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            when {
                isTalking -> "你正在讲话"
                remoteSpeakers.isNotEmpty() -> remoteSpeakers.joinToString("、") { it.nickname }
                speakers.any(IntercomMember::isMe) -> "你正在讲话"
                else -> "等待讲话"
            },
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(7.dp))
        Text(
            if (isActive) "声音实时传输中" else "声音已连接 · 可同时发言",
            color = Color.White.copy(alpha = .45f),
            fontSize = 12.sp
        )
    }
}

@Composable
internal fun PushToTalkButton(
    isTalking: Boolean,
    onTalkingChanged: (Boolean) -> Unit
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (isTalking) .94f else 1f,
        label = "pttScale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(112.dp)
            .background(if (isTalking) Color(0xFF19D672) else Color(0xFF07C160), CircleShape)
            .border(10.dp, Color.White.copy(alpha = .08f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onTalkingChanged(true)
                        tryAwaitRelease()
                        onTalkingChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_mic2_filled),
            "按住讲话",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
internal fun RoomControls(
    speakerEnabled: Boolean,
    onSpeakerToggle: () -> Unit,
    onMembers: () -> Unit,
    connectionLabel: String
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        RoundControl(
            icon = if (speakerEnabled) R.drawable.ic_speaker_filled else R.drawable.ic_speaker_off_filled,
            label = if (speakerEnabled) "扬声器" else "已静音",
            active = speakerEnabled,
            onClick = onSpeakerToggle
        )
        RoundControl(R.drawable.ic_group_chat_outlined, "成员", true, onMembers)
        RoundControl(R.drawable.ic_radar_outlined, connectionLabel, true, {})
    }
}

@Composable
private fun RoundControl(
    icon: Int,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = if (active) .1f else .04f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(icon),
                null,
                tint = Color.White.copy(alpha = if (active) .9f else .4f),
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = Color.White.copy(alpha = .55f), fontSize = 10.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MembersSheet(
    members: List<IntercomMember>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WeTheme.colorScheme.elevated
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "在线成员",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeTheme.colorScheme.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${members.size} 人",
                    color = WeTheme.colorScheme.textTertiary,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            if (members.isEmpty()) {
                Text(
                    "正在同步频道成员…",
                    color = WeTheme.colorScheme.textTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    textAlign = TextAlign.Center
                )
            }
            members.forEach { member ->
                val displayName = if (member.isMe) "${member.nickname}（你）" else member.nickname
                val status = if (member.isSpeaking) "正在讲话" else "在线"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .background(
                                if (member.isSpeaking) WeTheme.colorScheme.primary.copy(alpha = .16f) else WeTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            member.nickname.take(1),
                            color = if (member.isSpeaking) WeTheme.colorScheme.primary else WeTheme.colorScheme.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        displayName,
                        color = WeTheme.colorScheme.textPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedVisibility(member.isSpeaking, enter = fadeIn(), exit = fadeOut()) {
                        Icon(
                            painterResource(R.drawable.ic_voice_outlined),
                            null,
                            tint = WeTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        status,
                        color = if (member.isSpeaking) WeTheme.colorScheme.primary else WeTheme.colorScheme.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

