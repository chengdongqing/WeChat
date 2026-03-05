package top.chengdongqing.wechat.core.designsystem.util

import android.Manifest
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.call.model.CallType

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberCallLauncher(
    chatId: String,
    onStartCall: (String, CallType) -> Unit
): (CallType) -> Unit {
    var pendingCallType by remember { mutableStateOf<CallType?>(null) }

    // 定义所有可能需要的权限列表
    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    // 判断特定通话类型的权限是否已就绪
    fun isPermissionGranted(type: CallType): Boolean {
        val hasMic = permissionsState.permissions.any {
            it.permission == Manifest.permission.RECORD_AUDIO && it.status.isGranted
        }
        val hasCamera = permissionsState.permissions.any {
            it.permission == Manifest.permission.CAMERA && it.status.isGranted
        }
        return if (type == CallType.Video) (hasMic && hasCamera) else hasMic
    }

    // 监听权限状态变化
    LaunchedEffect(permissionsState.allPermissionsGranted, pendingCallType) {
        val type = pendingCallType ?: return@LaunchedEffect

        if (isPermissionGranted(type)) {
            onStartCall(chatId, type)
            pendingCallType = null
        }
    }

    return { type ->
        if (isPermissionGranted(type)) {
            onStartCall(chatId, type)
        } else {
            pendingCallType = type

            if (type.isVideoCall) {
                permissionsState.launchMultiplePermissionRequest()
            } else {
                // 如果是语音通话，只请求麦克风
                val micPermission = permissionsState.permissions.find {
                    it.permission == Manifest.permission.RECORD_AUDIO
                }
                micPermission?.launchPermissionRequest()
            }
        }
    }
}

val CallOptions = listOf(
    ActionSheetItem("视频通话", icon = {
        Icon(
            painter = painterResource(R.drawable.ic_video_filled),
            contentDescription = null,
            tint = WeTheme.colorScheme.textPrimary,
            modifier = Modifier.size(18.dp)
        )
    }),
    ActionSheetItem("语音通话", icon = {
        Icon(
            painter = painterResource(R.drawable.ic_call_filled),
            contentDescription = null,
            tint = WeTheme.colorScheme.textPrimary,
            modifier = Modifier.size(18.dp)
        )
    })
)