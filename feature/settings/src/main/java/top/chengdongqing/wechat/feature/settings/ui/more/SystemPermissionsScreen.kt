package top.chengdongqing.wechat.feature.settings.ui.more

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.settings.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.notification.CallNotificationPermissionManager
import top.chengdongqing.wechat.core.util.navigateToAppSettings
import top.chengdongqing.wechat.feature.settings.domain.model.RequiredPermission

@Composable
fun SystemPermissionSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val permissions = RequiredPermission.entries

    // 权限状态 Map
    var permissionStatusMap by remember {
        mutableStateOf(permissions.associateWith { false })
    }
    var fullScreenCallGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        // 每次应用回到前台时，重新检测所有权限
        permissionStatusMap = permissions.associateWith { permission ->
            context.isPermissionGranted(permission)
        }
        fullScreenCallGranted =
            CallNotificationPermissionManager.canUseFullScreenIntent(context)
        overlayGranted =
            CallNotificationPermissionManager.canDisplayOverOtherApps(context)
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.permission_title),
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBouncedOverscrollEffect()
                )
                .padding(innerPadding)
        ) {
            WeSettingGroup {
                permissions.forEachIndexed { index, permission ->
                    val isGranted = permissionStatusMap[permission] ?: false

                    WeSettingItem(
                        label = stringResource(permission.labelRes),
                        description = stringResource(permission.descriptionRes),
                        showDivider = index < permissions.lastIndex,
                        height = 68.dp,
                        onClick = {
                            context.navigateToAppSettings()
                        }
                    ) {
                        if (isGranted) {
                            Icon(
                                painter = painterResource(DesignR.drawable.ic_check),
                                contentDescription = stringResource(R.string.permission_enabled),
                                tint = WeTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.permission_full_screen_call),
                    description = stringResource(R.string.permission_full_screen_call_desc),
                    showDivider = true,
                    height = 68.dp,
                    onClick = {
                        CallNotificationPermissionManager.openFullScreenIntentSettings(context)
                    }
                ) {
                    if (fullScreenCallGranted) PermissionEnabledIcon()
                }
                WeSettingItem(
                    label = stringResource(R.string.permission_overlay_call),
                    description = stringResource(R.string.permission_overlay_call_desc),
                    showDivider = CallNotificationPermissionManager.needsMiuiCallPermissions(),
                    height = 68.dp,
                    onClick = {
                        CallNotificationPermissionManager.openOverlaySettings(context)
                    }
                ) {
                    if (overlayGranted) PermissionEnabledIcon()
                }
                if (CallNotificationPermissionManager.needsMiuiCallPermissions()) {
                    WeSettingItem(
                        label = stringResource(R.string.permission_miui_call),
                        description = stringResource(R.string.permission_miui_call_desc),
                        showDivider = false,
                        height = 68.dp,
                        onClick = {
                            CallNotificationPermissionManager.openMiuiPermissionSettings(context)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun PermissionEnabledIcon() {
    Icon(
        painter = painterResource(DesignR.drawable.ic_check),
        contentDescription = stringResource(R.string.permission_enabled),
        tint = WeTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
    )
}

/**
 * 权限检测逻辑
 */
fun Context.isPermissionGranted(permission: RequiredPermission): Boolean {
    val permissionString = when (permission) {
        RequiredPermission.Location -> Manifest.permission.ACCESS_FINE_LOCATION
        RequiredPermission.Microphone -> Manifest.permission.RECORD_AUDIO
        RequiredPermission.Camera -> Manifest.permission.CAMERA
        RequiredPermission.NFC -> return true // NFC主要靠硬件开启检测，权限通常在清单声明即可
        RequiredPermission.Bluetooth -> {
            // Android 12+ 需要扫描和连接权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            }
            Manifest.permission.BLUETOOTH
        }

        RequiredPermission.WiFi -> Manifest.permission.ACCESS_FINE_LOCATION
    }
    return ContextCompat.checkSelfPermission(
        this,
        permissionString
    ) == PackageManager.PERMISSION_GRANTED
}
