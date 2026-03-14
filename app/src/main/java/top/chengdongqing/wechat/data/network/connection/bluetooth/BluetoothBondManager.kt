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
import kotlin.coroutines.resume

@Singleton
class BluetoothBondManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val connectionInfoDao: ConnectionInfoDao,
    private val socketClient: SocketClient,
    @param:IoScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BluetoothBondManager"
    }

    @SuppressLint("MissingPermission")
    suspend fun bondAndConnect(userId: String, device: BluetoothDevice, myUserId: String) {
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                saveAndConnect(userId, device, myUserId)
            }

            BluetoothDevice.BOND_NONE -> {
                device.createBond()
                waitForBond(userId, device, myUserId)
            }

            BluetoothDevice.BOND_BONDING -> {
                waitForBond(userId, device, myUserId)
            }
        }
    }

    private suspend fun waitForBond(
        userId: String,
        device: BluetoothDevice,
        myUserId: String
    ) = suspendCancellableCoroutine { cont ->
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                val bondDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                } ?: return

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
        context.registerReceiver(
            receiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
        cont.invokeOnCancellation {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun saveAndConnect(userId: String, device: BluetoothDevice, myUserId: String) {
        connectionInfoDao.insertOrUpdate(
            ConnectionInfoEntity(
                userId = userId,
                bluetoothName = device.name,
                bluetoothAddress = device.address,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
        )
        socketClient.connect(
            userId = userId,
            macAddress = device.address,
            myUserId = myUserId
        ).onSuccess {
            Log.d(TAG, "RFCOMM 已连接: $userId")
        }.onFailure {
            Log.e(TAG, "RFCOMM 连接失败: $userId", it)
        }
    }
}