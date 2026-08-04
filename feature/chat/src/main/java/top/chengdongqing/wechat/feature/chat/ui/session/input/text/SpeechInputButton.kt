package top.chengdongqing.wechat.feature.chat.ui.session.input.text

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.playback.SpeechRecognizerManager
import top.chengdongqing.wechat.core.playback.SpeechState
import top.chengdongqing.wechat.core.playback.SpeechStatus

/**
 * 语音输入按钮
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeechInputButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val speechState = rememberSpeechInputState(onResult)

    Box(
        modifier = modifier
            .size(40.dp)
            .onTap(onClick = speechState::toggle),
        contentAlignment = Alignment.Center
    ) {
        if (speechState.isListening) {
            Icon(
                painter = painterResource(R.drawable.ic_mic_circle_outlined),
                contentDescription = "语音输入",
                tint = Color.White,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(WeTheme.colorScheme.primary)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_mic_outlined),
                contentDescription = "语音输入",
                tint = WeTheme.colorScheme.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
class SpeechInputState(
    private val manager: SpeechRecognizerManager,
    private val audioPermissionState: PermissionState
) {
    val state: SpeechState get() = manager.state.value
    val isListening: Boolean get() = state.status.isListening

    fun toggle() {
        when (state.status) {
            SpeechStatus.Idle, SpeechStatus.Error -> {
                if (!audioPermissionState.status.isGranted) {
                    audioPermissionState.launchPermissionRequest()
                } else {
                    manager.start()
                }
            }

            SpeechStatus.Listening -> manager.stop()
            else -> {}
        }
    }

    fun destroy() = manager.destroy()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberSpeechInputState(
    onResult: (String) -> Unit
): SpeechInputState {
    val context = LocalContext.current
    val manager = remember { SpeechRecognizerManager(context) }
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val state = remember(manager, audioPermissionState) {
        SpeechInputState(manager, audioPermissionState)
    }

    // 处理识别结果回调
    val currentState by manager.state.collectAsStateWithLifecycle()
    LaunchedEffect(currentState.finalResult) {
        if (currentState.finalResult.isNotEmpty()) {
            onResult(currentState.finalResult)
        }
    }

    // 自动销毁
    DisposableEffect(Unit) {
        onDispose { state.destroy() }
    }

    return state
}
