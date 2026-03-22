package top.chengdongqing.wechat.data.network.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.service.addfriend.BLEAddFriendHandler
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE Peripheral (server) role.
 *
 * Responsibilities:
 *  - Advertise the user's ID hash so nearby devices can discover us
 *  - Host a GATT server to receive [BLEPacket] writes from remote clients
 *  - Push [BLEPacket] notifications to subscribed remote clients
 *
 * Exposed flows (consumed by [BLEAddFriendHandler]):
 *  - [packets]       – raw incoming packets from any connected client
 *  - [subscriptions] – fires when a client enables notifications (signals it's ready to receive data)
 *  - [disconnections]– fires when a client disconnects
 */
@Singleton
@SuppressLint("MissingPermission")
class BLEServer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "BLEServer"
    }

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private var advertiser = bluetoothManager.adapter?.bluetoothLeAdvertiser
    private var gattServer: BluetoothGattServer? = null

    // Tracks connected device MAC addresses
    private val connectedDevices = ConcurrentHashMap.newKeySet<String>()

    private val _packets = MutableSharedFlow<ServerPacketEvent>(extraBufferCapacity = 64)
    val packets: SharedFlow<ServerPacketEvent> = _packets.asSharedFlow()

    private val _subscriptions = MutableSharedFlow<BluetoothDevice>(extraBufferCapacity = 8)
    val subscriptions: SharedFlow<BluetoothDevice> = _subscriptions.asSharedFlow()

    private val _disconnections = MutableSharedFlow<BluetoothDevice>(extraBufferCapacity = 8)
    val disconnections: SharedFlow<BluetoothDevice> = _disconnections.asSharedFlow()

    fun start(userIdHash: ByteArray) {
        startGattServer()
        startAdvertising(userIdHash)
    }

    fun stop() {
        runCatching {
            advertiser?.stopAdvertising(advertiseCallback)
            gattServer?.close()
        }
        gattServer = null
        connectedDevices.clear()
        Log.d(TAG, "BLE Server 已停止")
    }

    // ── Send (server → client via Notification) ───────────────────────────────

    /**
     * Sends a [BLEPacket] to [device] via GATT Notification (confirm = false).
     * The caller is responsible for chunking large payloads before calling this.
     */
    fun sendPacket(device: BluetoothDevice, packet: BLEPacket): Boolean {
        val server = gattServer ?: return false
        val char = server
            .getService(BLEConfig.SERVICE_UUID)
            ?.getCharacteristic(BLEConfig.CHARACTERISTIC_UUID) ?: return false
        return notifyCompat(server, device, char, packet.toBytes())
    }

    // ── Private: Advertising ──────────────────────────────────────────────────

    private fun startAdvertising(userIdHash: ByteArray) {
        val adapter = bluetoothManager.adapter?.takeIf { it.isEnabled } ?: run {
            Log.w(TAG, "蓝牙未启用，跳过广播")
            return
        }
        advertiser = adapter.bluetoothLeAdvertiser ?: run {
            Log.w(TAG, "设备不支持 BLE 广播")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BLEConfig.SERVICE_UUID))
            .addServiceData(ParcelUuid(BLEConfig.SERVICE_UUID), userIdHash)
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(s: AdvertiseSettings) {
            Log.d(TAG, "广播已启动")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "广播启动失败，错误码: $errorCode")
        }
    }

    // ── Private: GATT Server ──────────────────────────────────────────────────

    private fun startGattServer() {
        val characteristic = BluetoothGattCharacteristic(
            BLEConfig.CHARACTERISTIC_UUID,
            // PROPERTY_READ removed: we push data via NOTIFY, not pull
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        ).apply {
            addDescriptor(
                BluetoothGattDescriptor(
                    BLEConfig.DESCRIPTOR_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or
                            BluetoothGattDescriptor.PERMISSION_WRITE
                )
            )
        }

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback).apply {
            addService(
                BluetoothGattService(
                    BLEConfig.SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY
                ).also { it.addCharacteristic(characteristic) }
            )
        }
        Log.d(TAG, "GATT Server 已启动")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevices.add(device.address)
                    Log.d(TAG, "客户端已连接: ${device.address}")
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevices.remove(device.address)
                    scope.launch { _disconnections.emit(device) }
                    Log.d(TAG, "客户端已断开: ${device.address}")
                }
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice, requestId: Int,
            descriptor: BluetoothGattDescriptor, preparedWrite: Boolean,
            responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            if (descriptor.uuid != BLEConfig.DESCRIPTOR_UUID) return

            // Some clients require an explicit response, otherwise they time out
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }

            when {
                // Client enables notifications — consistent with ENABLE_NOTIFICATION_VALUE on the client side
                value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) -> {
                    scope.launch { _subscriptions.emit(device) }
                    Log.d(TAG, "客户端已订阅: ${device.address}")
                }

                value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) -> {
                    Log.d(TAG, "客户端已取消订阅: ${device.address}")
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int,
            characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean,
            responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            // Reject writes from devices that never established a connection state
            if (!connectedDevices.contains(device.address)) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
                return
            }

            val status = runCatching { BLEPacket.fromBytes(value) }
                .onSuccess { packet ->
                    scope.launch { _packets.emit(ServerPacketEvent(device, packet)) }
                }
                .fold(
                    onSuccess = { BluetoothGatt.GATT_SUCCESS },
                    onFailure = {
                        Log.e(TAG, "解析 BlePacket 失败: ${it.message}")
                        BluetoothGatt.GATT_FAILURE
                    }
                )

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, status, 0, null)
            }
        }
    }

    // ── Private: Compat ───────────────────────────────────────────────────────

    private fun notifyCompat(
        server: BluetoothGattServer,
        device: BluetoothDevice,
        char: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            server.notifyCharacteristicChanged(
                device,
                char,
                false,
                value
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            char.value = value
            @Suppress("DEPRECATION")
            server.notifyCharacteristicChanged(device, char, false)
        }
    }.getOrDefault(false)
}

/**
 * An incoming BlePacket from a specific remote client.
 */
data class ServerPacketEvent(
    val device: BluetoothDevice,
    val packet: BLEPacket,
)