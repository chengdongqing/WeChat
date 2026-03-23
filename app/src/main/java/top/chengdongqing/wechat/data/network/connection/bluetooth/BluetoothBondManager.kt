package top.chengdongqing.wechat.data.network.connection.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * 蓝牙配对与连接管理器。
 *
 * 负责处理配对状态机（未配对 → 配对中 → 已配对）并在配对成功后
 * 建立 RFCOMM 连接，同时将连接信息持久化供后续重连使用。
 */
@Singleton
@SuppressLint("MissingPermission")
class BluetoothBondManager @Inject constructor(
    private val connectionInfoDao: ConnectionInfoDao,
    private val socketClient: BluetoothSocketClient,
    @param:IoScope private val scope: CoroutineScope,
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BtBondManager"
    }

    /**
     * 根据当前配对状态决策：
     * - 已配对：直接保存并连接
     * - 未配对：发起配对请求，等待系统广播结果
     * - 配对中：说明其他地方已触发配对，直接挂起等待结果
     */
    suspend fun bondAndConnect(userId: String, device: BluetoothDevice, myUserId: String) {
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> saveAndConnect(userId, device, myUserId)
            BluetoothDevice.BOND_NONE -> {
                device.createBond()
                waitForBond(userId, device, myUserId)
            }

            BluetoothDevice.BOND_BONDING -> waitForBond(userId, device, myUserId)
        }
    }

    /**
     * 挂起当前协程，监听 [BluetoothDevice.ACTION_BOND_STATE_CHANGED] 广播，
     * 直到目标设备配对成功或被拒绝。
     *
     * - 配对成功：保存连接信息并建立 RFCOMM 连接，然后恢复协程
     * - 用户拒绝：取消协程，调用方会收到 [CancellationException]
     * - 协程被外部取消：注销广播接收器，防止泄漏
     */
    private suspend fun waitForBond(
        userId: String,
        device: BluetoothDevice,
        myUserId: String,
    ) = suspendCancellableCoroutine { cont ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // 过滤掉非目标设备的广播（系统可能同时扫描多台设备）
                val bondDevice = intent.bluetoothDevice ?: return
                if (bondDevice.address != device.address) return

                when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)) {
                    BluetoothDevice.BOND_BONDED -> {
                        context.unregisterReceiver(this)
                        scope.launch { saveAndConnect(userId, bondDevice, myUserId) }
                        if (cont.isActive) cont.resume(Unit)
                    }

                    BluetoothDevice.BOND_NONE -> {
                        Log.w(TAG, "用户拒绝配对: $userId")
                        context.unregisterReceiver(this)
                        if (cont.isActive) cont.cancel()
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
        cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
    }

    /**
     * 保存连接信息，并建立 RFCOMM Socket 连接
     */
    private suspend fun saveAndConnect(userId: String, device: BluetoothDevice, myUserId: String) {
        saveToDB(userId, device)

        socketClient.connect(userId = userId, macAddress = device.address, myUserId = myUserId)
            .onSuccess { Log.d(TAG, "RFCOMM 已连接: $userId") }
            .onFailure { Log.e(TAG, "RFCOMM 连接失败: $userId", it) }
    }

    /**
     * 保存连接信息到数据库
     */
    suspend fun saveToDB(userId: String, device: BluetoothDevice) {
        connectionInfoDao.upsert(
            ConnectionInfoEntity(
                userId = userId,
                bluetoothName = device.name,
                bluetoothAddress = device.address,
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
            )
        )
    }

    /**
     * 是否保存过
     */
    suspend fun hasSaved(userId: String): Boolean =
        connectionInfoDao.getById(userId)?.bluetoothAddress != null
}

val Intent.bluetoothDevice: BluetoothDevice?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }