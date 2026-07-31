package top.chengdongqing.wechat.feature.chat.ui.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.location.WeMap
import top.chengdongqing.wechat.core.location.heading.rememberDeviceHeading
import top.chengdongqing.wechat.core.location.map.MapMarkerHandle
import top.chengdongqing.wechat.core.location.rememberMapController
import top.chengdongqing.wechat.core.util.showToast

@Composable
fun LiveLocationScreen(
    onBack: () -> Unit,
    viewModel: LiveLocationViewModel
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val controller = rememberMapController()
    val remote by viewModel.remoteLocation.collectAsStateWithLifecycle()
    val remoteBearing by viewModel.remoteBearing.collectAsStateWithLifecycle()
    val deviceBearing by rememberDeviceHeading()
    val participants by viewModel.participants.collectAsStateWithLifecycle()
    var remoteMarker by remember { mutableStateOf<MapMarkerHandle?>(null) }
    var talking by remember { mutableStateOf(false) }
    var centeredOnMe by remember { mutableStateOf(false) }
    val remoteLocationIcon = remember {
        BitmapFactory.decodeResource(resources, R.drawable.img_your_location)
    }
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            talking = viewModel.audio.setTransmitting(true)
        } else {
            context.showToast(resources.getString(R.string.live_location_permission))
        }
    }

    LaunchedEffect(controller) {
        controller.setOnLocationChangeListener { point, bearing ->
            viewModel.updateLocation(point, bearing)
            if (!centeredOnMe) {
                centeredOnMe = true
                controller.moveTo(point, 17f)
            }
        }
        viewModel.start()
        controller.currentLocation?.let {
            viewModel.updateLocation(it, controller.currentBearing)
            centeredOnMe = true
            controller.moveTo(it, 17f)
        }
    }
    LaunchedEffect(deviceBearing) {
        deviceBearing?.let(viewModel::updateBearing)
    }
    LaunchedEffect(remote, remoteBearing) {
        val point = remote
        if (point == null) {
            remoteMarker?.remove()
            remoteMarker = null
        } else {
            val marker = remoteMarker
            if (marker == null) {
                remoteMarker = controller.addMarker(
                    point,
                    remoteLocationIcon,
                    remoteBearing ?: 0f
                ).also {
                    controller.moveTo(point, 15f)
                }
            } else {
                marker.update(point, remoteBearing)
            }
        }
    }
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            remoteMarker?.remove()
            viewModel.stop()
        }
    }

    Box(Modifier.fillMaxSize()) {
        WeMap(
            controller = controller,
            modifier = Modifier.fillMaxSize()
        )
        LiveLocationHeader(
            participants = participants,
            onClose = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        TalkButton(
            talking = talking,
            onStart = {
                if (viewModel.audio.canRecord()) {
                    talking = viewModel.audio.setTransmitting(true)
                } else {
                    microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStop = {
                viewModel.audio.setTransmitting(false)
                talking = false
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        )
    }
}

@Composable
private fun LiveLocationHeader(
    participants: List<LiveLocationParticipantUi>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC696969))
            .padding(top = 50.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 22.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xBB303030))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_close_outlined),
                    contentDescription = null,
                    tint = Color(0xFF35DB87),
                    modifier = Modifier.size(25.dp)
                )
            }
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                participants.take(5).forEach { participant ->
                    AsyncImage(
                        model = participant.avatarPath,
                        contentDescription = participant.name,
                        error = painterResource(R.drawable.img_avatar_placeholder),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.live_location_people, participants.size),
            modifier = Modifier.padding(top = 9.dp),
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TalkButton(
    talking: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(if (talking) Color(0xFFE95656) else Color(0xFF07C160), CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onStart()
                        waitForUpOrCancellation()
                        onStop()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_mic2_filled),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            text = if (talking) {
                stringResource(R.string.live_location_talking)
            } else {
                stringResource(R.string.live_location_talk)
            },
            modifier = Modifier.padding(top = 8.dp),
            color = WeTheme.colorScheme.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
