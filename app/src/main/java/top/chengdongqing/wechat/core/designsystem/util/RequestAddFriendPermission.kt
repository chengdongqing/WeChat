package top.chengdongqing.wechat.core.designsystem.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.data.network.service.NetworkService
import top.chengdongqing.wechat.data.network.service.createNetworkServiceIntent

@SuppressLint("MissingPermission")
@Composable
fun RequestAddFriendPermission(
    extraPermissions: List<String> = emptyList(),
    onRevoked: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val locationManager =
        remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val bluetoothManager =
        remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val bluetoothAdapter = bluetoothManager.adapter

    // 定义状态
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }
    var isLocationEnabled by remember {
        mutableStateOf(LocationManagerCompat.isLocationEnabled(locationManager))
    }

    // 从设置返回时刷新状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // 用户回到 App 时，强制重新检查开关状态
            if (event == Lifecycle.Event.ON_RESUME) {
                isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
                isLocationEnabled = LocationManagerCompat.isLocationEnabled(locationManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 根据状态渲染 UI
    when {
        bluetoothAdapter == null -> {
            LaunchedEffect(Unit) { context.showToast("此设备不支持蓝牙") }
            onRevoked?.invoke()
        }

        !isBluetoothEnabled -> {
            BluetoothEnableGuide {
                context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }

        !isLocationEnabled -> {
            LocationEnableGuide {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }

        else -> {
            val permissions = remember(extraPermissions) {
                AddFriendPermissions + extraPermissions
            }

            // 开关都开了，才进入权限检查包装器
            PermissionWrapper(
                permissions = permissions,
                onRevoked = onRevoked,
                onGranted = {
                    // 启动蓝牙服务
                    val intent = context.createNetworkServiceIntent(NetworkService.ACTION_RETRY_BLE)
                    context.startService(intent)
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    content()
                }
            }
        }
    }
}

val AddFriendPermissions by lazy {
    buildList {
        // 蓝牙+定位权限
        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        addAll(list)
        // 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun BluetoothEnableGuide(onEnableClick: () -> Unit) {
    StatusGuideScreen(
        iconRes = R.drawable.ic_bluetooth_outlined,
        title = "需开启蓝牙",
        desc = "添加好友依赖蓝牙功能搜索附近的设备，请在设置中开启。",
        buttonText = "去开启",
        onButtonClick = onEnableClick
    )
}

@Composable
fun LocationEnableGuide(onEnableClick: () -> Unit) {
    StatusGuideScreen(
        iconRes = R.drawable.ic_location_filled,
        title = "需开启位置服务",
        desc = "Android 系统要求在搜索蓝牙设备时必须开启位置服务总开关。",
        buttonText = "去设置",
        onButtonClick = onEnableClick
    )
}

@Composable
fun StatusGuideScreen(
    iconRes: Int,
    title: String,
    desc: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = desc,
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        WeButton(
            text = buttonText,
            onClick = onButtonClick,
            type = ButtonType.Primary
        )
    }
}