package top.chengdongqing.wechat.ui.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallPermissionWrapper(
    onPermissionsGranted: @Composable () -> Unit
) {
    // 1. 定义需要请求的权限列表
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    )

    // 2. 根据权限状态展示不同 UI
    when {
        permissionState.allPermissionsGranted -> {
            // 全部授权成功，进入通话界面
            onPermissionsGranted()
        }

        permissionState.shouldShowRationale || !permissionState.allPermissionsGranted -> {
            // 权限被拒绝过，或者尚未请求
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("通话需要摄像头和麦克风权限", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                        Text("授权并开始通话")
                    }
                }
            }
        }
    }
}