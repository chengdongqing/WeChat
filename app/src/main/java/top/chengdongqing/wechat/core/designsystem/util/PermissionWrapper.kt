package top.chengdongqing.wechat.core.designsystem.util

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(
    permissions: List<String>,
    onRevoked: (() -> Unit)? = null,
    onGranted: (() -> Unit)? = null,
    autoRequest: Boolean = true,
    content: @Composable () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(permissions) { res ->
        if (res.values.any { !it }) {
            onRevoked?.invoke()
        }
    }

    // 自动请求逻辑
    LaunchedEffect(permissionState) {
        if (autoRequest && !permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    LaunchedUpdateEffect(permissionState) {
        if (permissionState.allPermissionsGranted) {
            onGranted?.invoke()
        }
    }

    if (permissionState.allPermissionsGranted) {
        content()
    }
}

@Composable
fun RequestMediaPermission(
    extraPermissions: List<String> = emptyList(),
    onRevoked: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            addAll(extraPermissions)
        }
    }

    PermissionWrapper(
        permissions = permissions,
        onRevoked = onRevoked,
        content = content
    )
}

@Composable
fun RequestCameraPermission(
    extraPermissions: List<String> = emptyList(),
    onRevoked: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val permissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            addAll(extraPermissions)
        }
    }

    PermissionWrapper(
        permissions = permissions,
        onRevoked = onRevoked
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ImmersiveSystemBars()
            content()
        }
    }
}