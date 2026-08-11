package top.chengdongqing.wechat.feature.intercom.ui

import android.Manifest
import android.icu.text.ListFormatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.intercom.R
import top.chengdongqing.wechat.feature.intercom.model.IntercomMember
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IntercomRoomScreen(
    channel: String,
    onBack: () -> Unit,
    viewModel: IntercomViewModel = hiltViewModel()
) {
    var isTalking by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }
    var microphoneDenied by remember { mutableStateOf(false) }
    val roomState by viewModel.roomState.collectAsStateWithLifecycle()
    val microphonePermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    DisposableEffect(channel) {
        viewModel.enterRoom(channel)
        onDispose {
            viewModel.leave()
        }
    }

    Scaffold(
        topBar = { WeTopAppBar(title = "#$channel", onBack = onBack) },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnlineMembers(
                memberCount = roomState.members.size,
                onClick = { showMembers = true }
            )
            Spacer(Modifier.height(38.dp))
            SpeakingStage(
                isTalking = isTalking,
                speakers = roomState.speakers
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = when {
                    isTalking -> stringResource(R.string.intercom_sending_voice)
                    roomState.speakers.isEmpty() -> ""
                    else -> pluralStringResource(
                        R.plurals.intercom_people_speaking,
                        roomState.speakers.size,
                        roomState.speakers.size
                    )
                },
                color = if (isTalking) {
                    WeTheme.colorScheme.primary
                } else {
                    WeTheme.colorScheme.textSecondary
                },
                fontSize = 13.sp
            )
            Spacer(Modifier.height(18.dp))
            PushToTalkButton(
                isTalking = isTalking,
                onTalkingChanged = {
                    when {
                        it && !microphonePermissionState.status.isGranted -> {
                            microphoneDenied = true
                            microphonePermissionState.launchPermissionRequest()
                        }

                        else -> {
                            isTalking = viewModel.setSpeaking(it) && it
                            if (it) {
                                microphoneDenied = false
                            }
                        }
                    }
                }
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (microphoneDenied) {
                    stringResource(R.string.intercom_microphone_permission_denied)
                } else {
                    stringResource(R.string.intercom_push_to_talk)
                },
                color = if (microphoneDenied) {
                    WeTheme.colorScheme.danger
                } else {
                    WeTheme.colorScheme.textSecondary
                },
                fontSize = 12.sp
            )
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(20.dp)
            )
        }
    }

    if (showMembers) {
        MembersSheet(
            members = roomState.members,
            onDismiss = { showMembers = false }
        )
    }
}

@Composable
private fun OnlineMembers(
    memberCount: Int,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(Color(0xFF43D17A), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = pluralStringResource(
                R.plurals.intercom_people_online,
                memberCount,
                memberCount
            ),
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 12.sp
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            painter = painterResource(DesignR.drawable.ic_right_outlined),
            contentDescription = null,
            tint = WeTheme.colorScheme.textSecondary,
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun PushToTalkButton(
    isTalking: Boolean,
    onTalkingChanged: (Boolean) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isTalking) .94f else 1f,
        label = "PushToTalkButtonScale"
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
            painter = painterResource(DesignR.drawable.ic_mic2_filled),
            contentDescription = stringResource(R.string.intercom_push_to_talk),
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersSheet(
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
                    stringResource(R.string.intercom_online_members_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeTheme.colorScheme.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    pluralStringResource(
                        R.plurals.intercom_people_count,
                        members.size,
                        members.size
                    ),
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            if (members.isEmpty()) {
                Text(
                    stringResource(R.string.intercom_syncing_members),
                    color = WeTheme.colorScheme.textTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    textAlign = TextAlign.Center
                )
            }
            members.forEach { member ->
                val displayName = if (member.isMe) {
                    stringResource(R.string.intercom_nickname_me, member.nickname)
                } else {
                    member.nickname
                }
                val status = if (member.isSpeaking) {
                    stringResource(R.string.intercom_status_speaking)
                } else {
                    stringResource(R.string.intercom_status_online)
                }

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
                            painterResource(DesignR.drawable.ic_voice_outlined),
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

@Composable
private fun SpeakingStage(
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

    val locale = LocalConfiguration.current.locales[0]
    val speakerNames = remember(remoteSpeakers, locale) {
        ListFormatter.getInstance(locale)
            .format(remoteSpeakers.map { it.nickname })
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(148.dp)
                    .graphicsLayer {
                        scaleX = if (isActive) pulse else 1f
                        scaleY = if (isActive) pulse else 1f
                    }
                    .background(
                        color = WeTheme.colorScheme.primary.copy(alpha = if (isActive) .18f else .08f),
                        shape = CircleShape
                    )
            )
            Box(
                Modifier
                    .size(116.dp)
                    .background(WeTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTalking) {
                        stringResource(R.string.intercom_me_short)
                    } else {
                        primarySpeaker?.nickname?.take(1) ?: "—"
                    },
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = when {
                isTalking -> stringResource(R.string.intercom_you_are_speaking)
                remoteSpeakers.isNotEmpty() -> speakerNames
                else -> stringResource(R.string.intercom_waiting_to_speak)
            },
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}
