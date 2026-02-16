package top.chengdongqing.wechat.features.chat.ui.session.input.voice

import android.Manifest
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatContext

/**
 * 语音录制按钮
 *
 * @param onVoiceSend 发送语音回调 (文件URI, 录音时长毫秒)
 * @param onConvertToText 转文字回调
 * @param minDuration 最小录音时长（毫秒）
 * @param maxDuration 最大录音时长（毫秒）
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecordButton(
    onVoiceSend: (uri: Uri, duration: Long) -> Unit,
    onConvertToText: (uri: Uri, duration: Long) -> Unit,
    minDuration: Long = 1000,
    maxDuration: Long = 60000
) {
    val chatContext = LocalChatContext.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // ========== 状态管理 ==========
    var isRecording by remember { mutableStateOf(false) }
    var recordState by remember { mutableStateOf(RecordState.Idle) }
    var recordDuration by remember { mutableLongStateOf(0L) }
    var audioAmplitude by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val audioRecorder = remember { AudioRecorderManager(context) }
    val focusManager = remember { AudioFocusManager(context) }

    // ========== 录音计时器 ==========
    // 每50ms更新一次时长和音量
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDuration = 0L
            while (isRecording) {
                delay(50)
                recordDuration += 50
                audioAmplitude = audioRecorder.getAmplitude()

                // 达到最大时长自动停止
                if (recordDuration >= maxDuration) {
                    handleRecordingComplete(
                        audioRecorder = audioRecorder,
                        recordDuration = recordDuration,
                        onSend = onVoiceSend
                    )
                    isRecording = false
                    recordState = RecordState.Idle
                }
            }
        }
    }

    // 录音权限管理
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        // 判断录音权限
                        if (!audioPermissionState.status.isGranted) {
                            audioPermissionState.launchPermissionRequest()
                            return@detectDragGesturesAfterLongPress
                        }
                        // 申请焦点，暂停其他App的音频播放
                        focusManager.requestFocus()
                        // 停止当前播放的语音
                        chatContext?.onVoiceStop?.invoke()

                        startRecording(audioRecorder) { success ->
                            if (success) {
                                isRecording = true
                                recordState = RecordState.Recording
                                recordDuration = 0L
                                audioAmplitude = 0f
                                dragOffset = Offset.Zero
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isRecording) {
                            change.consume()
                            dragOffset += dragAmount

                            // 根据手指位置更新状态
                            recordState = calculateRecordState(
                                dragOffset = dragOffset,
                                density = density
                            )
                        }
                    },
                    onDragEnd = {
                        if (isRecording) {
                            scope.launch {
                                handleDragEnd(
                                    recordState = recordState,
                                    recordDuration = recordDuration,
                                    minDuration = minDuration,
                                    audioRecorder = audioRecorder,
                                    onSend = onVoiceSend,
                                    onConvertToText = onConvertToText,
                                    onStateChange = { newState ->
                                        recordState = newState
                                        isRecording = newState != RecordState.Idle
                                    }
                                )

                                // 取消焦点
                                focusManager.abandonFocus()
                            }
                        }
                    },
                    onDragCancel = {
                        audioRecorder.cancelRecording()
                        isRecording = false
                        recordState = RecordState.Idle
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 中心提示文字
        RecordButtonText(recordState)
    }

    // ========== 录音遮罩层 ==========
    if (isRecording && recordState != RecordState.TooShort) {
        RecordingDialog(
            recordState = recordState,
            audioAmplitude = audioAmplitude
        )
    }
}

// ==================== 辅助函数 ====================

/**
 * 开始录音
 */
private fun startRecording(
    audioRecorder: AudioRecorderManager,
    onResult: (Boolean) -> Unit
) {
    val success = audioRecorder.startRecording()
    onResult(success)
}

/**
 * 计算录音状态
 *
 * 滑动检测逻辑：
 * 1. 向上滑动超过100dp进入按钮区域
 * 2. 向左偏移超过50dp → 取消区域
 * 3. 向右偏移超过50dp → 转文字区域
 * 4. 其他情况 → 正常录音
 */
private fun calculateRecordState(
    dragOffset: Offset,
    density: Density
): RecordState {
    val offsetYDp = with(density) { dragOffset.y.toDp() }
    val offsetXDp = with(density) { dragOffset.x.toDp() }

    return when {
        offsetYDp < (-100).dp -> {
            when {
                offsetXDp < (-50).dp -> RecordState.Cancel
                offsetXDp > 50.dp -> RecordState.Convert
                else -> RecordState.Recording
            }
        }

        else -> RecordState.Recording
    }
}

/**
 * 处理拖动结束
 */
private suspend fun handleDragEnd(
    recordState: RecordState,
    recordDuration: Long,
    minDuration: Long,
    audioRecorder: AudioRecorderManager,
    onSend: (Uri, Long) -> Unit,
    onConvertToText: ((Uri, Long) -> Unit)?,
    onStateChange: (RecordState) -> Unit
) {
    when (recordState) {
        RecordState.Cancel -> {
            audioRecorder.cancelRecording()
            onStateChange(RecordState.Idle)
        }

        RecordState.Convert -> {
            val uri = audioRecorder.stopRecording()
            if (uri != null && onConvertToText != null) {
                onConvertToText(uri, recordDuration)
            }
            onStateChange(RecordState.Idle)
        }

        RecordState.Recording -> {
            if (recordDuration < minDuration) {
                // 时间太短
                audioRecorder.cancelRecording()
                onStateChange(RecordState.TooShort)
                delay(1200)
                onStateChange(RecordState.Idle)
            } else {
                // 正常发送
                val uri = audioRecorder.stopRecording()
                if (uri != null) {
                    onSend(uri, recordDuration)
                }
                onStateChange(RecordState.Idle)
            }
        }

        else -> {}
    }
}

/**
 * 处理录音完成
 */
private fun handleRecordingComplete(
    audioRecorder: AudioRecorderManager,
    recordDuration: Long,
    onSend: (Uri, Long) -> Unit
) {
    val uri = audioRecorder.stopRecording()
    if (uri != null) {
        onSend(uri, recordDuration)
    }
}

// ==================== UI组件 ====================

/**
 * 录音按钮文字
 */
@Composable
private fun RecordButtonText(recordState: RecordState) {
    Text(
        text = when (recordState) {
            RecordState.TooShort -> "说话时间太短"
            else -> "按住 说话"
        },
        color = if (recordState == RecordState.TooShort) {
            Color(0xFFFF3B30)
        } else {
            Color.Black
        },
        fontSize = 16.sp
    )
}

/**
 * 录音对话框（全屏）
 */
@Composable
private fun RecordingDialog(
    recordState: RecordState,
    audioAmplitude: Float
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            RecordingOverlay(
                recordState = recordState,
                audioAmplitude = audioAmplitude
            )
        }
    }
}

/**
 * 录音状态枚举
 */
enum class RecordState {
    Idle,       // 空闲
    Recording,  // 正常录音
    Cancel,     // 准备取消
    Convert,    // 准备转文字
    TooShort   // 时间太短
}