package top.chengdongqing.wechat.ui.chat.session.input.voice

import android.net.Uri
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VoiceRecordButton(
    onSend: (uri: Uri, duration: Long) -> Unit
) {
    val context = LocalContext.current
    val recorderManager = remember { VoiceRecordManager(context) }

    var status by remember { mutableStateOf(RecordStatus.IDLE) }
    var amplitude by remember { mutableFloatStateOf(0f) } // 振幅 (0.0 ~ 1.0)
    var startTime by remember { mutableLongStateOf(0L) }
    var currentPath by remember { mutableStateOf(Uri.EMPTY) }

    // 振幅轮询协程
    LaunchedEffect(status) {
        if (status != RecordStatus.IDLE) {
            while (isActive) {
                amplitude = recorderManager.getAmplitude()
                delay(100) // 100ms 更新一次声纹动画
            }
        } else {
            amplitude = 0f
        }
    }

    Box(contentAlignment = Alignment.BottomCenter) {
        // 1. 顶部覆盖层：根据状态显示声纹、取消图标或转文字图标
        RecordOverlay(status, amplitude)

        // 2. 实际的按钮
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            try {
                                startTime = System.currentTimeMillis()
                                val path = recorderManager.startRecording()
                                if (path != null) {
                                    currentPath = path.toUri()
                                    status = RecordStatus.RECORDING
                                }
                            } catch (e: Exception) {
                                // onToast("无法开启录音机")
                            }

                            // 等待手指释放
                            val success = tryAwaitRelease()
                            val duration = System.currentTimeMillis() - startTime

                            if (success) {
                                when (status) {
                                    RecordStatus.RECORDING -> {
                                        if (duration < 800) { // 微信通常是少于1秒不发
                                            // onToast("录音时间太短")
                                            recorderManager.cancelRecording()
                                        } else {
                                            recorderManager.stopRecording()
                                            onSend(currentPath, duration)
                                        }
                                    }

                                    RecordStatus.CANCELING -> {
                                        recorderManager.cancelRecording()
                                    }

                                    RecordStatus.TRANSING -> {
                                        // 这里处理转文字逻辑
                                        recorderManager.stopRecording()
                                        // onToast("转文字功能开发中")
                                    }

                                    else -> recorderManager.cancelRecording()
                                }
                            } else {
                                recorderManager.cancelRecording()
                            }
                            status = RecordStatus.IDLE
                        }
                    )
                }
                // 这里复用之前的 detectDragGestures 逻辑来更新 status 变量...
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            val yOffset = change.position.y
                            // 向上滑动超过 150 像素进入取消/转文字判定
                            if (yOffset < -150) {
                                status = if (change.position.x < size.width / 2)
                                    RecordStatus.CANCELING else RecordStatus.TRANSING
                            } else {
                                status = RecordStatus.RECORDING
                            }
                        },
                        onDragEnd = { /* 已经在 onPress 处理了 */ }
                    )
                },
            color = if (status == RecordStatus.IDLE) Color(0xFFF7F7F7) else Color(0xFFE5E5E5),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (status == RecordStatus.IDLE) "按住 说话" else "松开 结束",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                )
            }
        }
    }
}