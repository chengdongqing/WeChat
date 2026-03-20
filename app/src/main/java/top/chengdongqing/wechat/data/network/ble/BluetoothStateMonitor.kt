package top.chengdongqing.wechat.data.network.ble

import android.Manifest
import android.app.AppOpsManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 蓝牙状态监听器
 *
 * 同时监控两个维度：
 * 1. 蓝牙开关（通过系统广播 [BluetoothAdapter.ACTION_STATE_CHANGED]）
 * 2. 运行时权限（通过 [AppOpsManager.OnOpChangedListener]）
 *
 * 两者均满足时 [isAvailable] 才为 true，P2PService 据此自动启停 BLE 模块
 */
@Singleton
class BluetoothStateMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BluetoothStateMonitor"

        /**
         * Android 12（S）起必须持有 BLUETOOTH_CONNECT 权限才能操作蓝牙。
         * 旧版本只需 BLUETOOTH 权限，安装即授予，无需运行时检查。
         */
        private val REQUIRED_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }

        /**
         * AppOpsManager 中与 BLUETOOTH_CONNECT 对应的 Op 名称（Android 12+）。
         */
        private const val BLUETOOTH_CONNECT_OP = "android:bluetooth_connect"
    }

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val _isAvailable = MutableStateFlow(checkAvailability())

    /**
     * 蓝牙是否可用：已开启 + 已授权。
     * 在 [start] 之后持续更新，在 [stop] 之后停止。
     */
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    /**
     * 蓝牙开关监听（广播）
     */
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return

            val state = intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR
            )
            Log.d(TAG, "蓝牙状态变更: $state")
            refresh()
        }
    }

    /**
     * 权限监听器
     */
    private val permissionOpsListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AppOpsManager.OnOpChangedListener { op, _ ->
            if (op == BLUETOOTH_CONNECT_OP) {
                Log.d(TAG, "蓝牙权限变更: $op")
                refresh()
            }
        }
    } else null

    /**
     * 开始监听
     */
    fun start() {
        // 注册蓝牙开关广播
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        // 注册权限变化监听（仅 Android 12+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && permissionOpsListener != null) {
            context.getSystemService<AppOpsManager>()?.startWatchingMode(
                BLUETOOTH_CONNECT_OP,
                context.packageName,
                permissionOpsListener
            )
        }

        // 立即刷新一次，确保初始状态准确
        refresh()

        Log.d(TAG, "蓝牙状态监听已启动，当前可用: ${_isAvailable.value}")
    }

    /**
     * 停止监听
     */
    fun stop() {
        runCatching { context.unregisterReceiver(bluetoothStateReceiver) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                permissionOpsListener?.let { listener ->
                    context.getSystemService<AppOpsManager>()?.stopWatchingMode(listener)
                }
            }
        }

        Log.d(TAG, "蓝牙状态监听已停止")
    }

    /**
     * 主动刷新当前状态，外部可在申请权限后调用以立即生效
     */
    fun refresh() {
        _isAvailable.value = checkAvailability()
        Log.d(TAG, "蓝牙可用状态刷新: ${_isAvailable.value}")
    }

    /**
     * 判断蓝牙当前是否可用：开关已开启 && 权限已授予
     */
    private fun checkAvailability(): Boolean {
        val isOn = bluetoothAdapter?.isEnabled == true
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "蓝牙检查 → 开关=$isOn, 权限=$hasPermission")
        return isOn && hasPermission
    }
}