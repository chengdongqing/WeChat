package top.chengdongqing.wechat.core.call.ui

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
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.call.ui.R as CallUiR

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberCallLauncher(
    chatId: String,
    onStartCall: (String, CallType) -> Unit
): (CallType) -> Unit {
    var pendingCallType by remember { mutableStateOf<CallType?>(null) }
    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    fun isPermissionGranted(type: CallType): Boolean {
        val hasMic = permissionsState.permissions.any {
            it.permission == Manifest.permission.RECORD_AUDIO && it.status.isGranted
        }
        val hasCamera = permissionsState.permissions.any {
            it.permission == Manifest.permission.CAMERA && it.status.isGranted
        }
        return if (type == CallType.Video) hasMic && hasCamera else hasMic
    }

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
                permissionsState.permissions
                    .find { it.permission == Manifest.permission.RECORD_AUDIO }
                    ?.launchPermissionRequest()
            }
        }
    }
}

val CallOptions = listOf(
    ActionSheetItem(
        labelRes = CallUiR.string.chat_call_video,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_video_filled),
                contentDescription = null,
                tint = WeTheme.colorScheme.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    ),
    ActionSheetItem(
        labelRes = CallUiR.string.chat_call_voice,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_call_filled),
                contentDescription = null,
                tint = WeTheme.colorScheme.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    )
)
