package top.chengdongqing.wechat.core.network.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Manages a single active GATT client connection.
 *
 * Lifecycle:
 *   [connect] → [sendPacket] / [subscribeToNotifications] → collect [packets] → [close]
 *
 * Each [BLEConnection] owns its own [CoroutineScope] (child of the parent passed in).
 * Calling [close] cancels that scope, cleaning up all ongoing operations.
 *
 * Not a singleton — created per-connection by [BLEConnectionManager].
 */
@SuppressLint("MissingPermission")
class BLEConnection(
    private val context: Context,
    parentScope: CoroutineScope,
) {
    companion object {
        private const val TAG = "BLEConnection"
    }

    // Own scope: canceled on close, does not affect the parent scope
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)

    private var gatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    private var connectContinuation: CancellableContinuation<Boolean>? = null
    private var writeContinuation: CancellableContinuation<Boolean>? = null

    /** Incoming BlePackets pushed by the remote peripheral via Notification. */
    private val _packets = MutableSharedFlow<BLEPacket>(extraBufferCapacity = 64)
    val packets: SharedFlow<BLEPacket> = _packets.asSharedFlow()

    /**
     * Connects to [device], negotiates MTU, and discovers services.
     * @return true if the target [BLEConfig.CHARACTERISTIC_UUID] was found.
     */
    suspend fun connect(device: BluetoothDevice): Boolean {
        return withTimeoutOrNull(BLEConfig.CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                connectContinuation = cont
                gatt = device.connectGatt(
                    context,
                    /*autoConnect=*/ false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE,
                )
                cont.invokeOnCancellation { closeGatt() }
            }
        } ?: run {
            Log.w(TAG, "连接超时: ${device.address}")
            false
        }
    }

    /**
     * Writes a single [BLEPacket] to the remote peripheral (WRITE_WITH_RESPONSE).
     * Suspends until [BluetoothGattCallback.onCharacteristicWrite] fires.
     */
    suspend fun sendPacket(packet: BLEPacket): Boolean {
        val g = gatt ?: return false
        val c = characteristic ?: return false

        return withTimeoutOrNull(BLEConfig.WRITE_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                writeContinuation = cont
                val started = writeCharacteristicCompat(g, c, packet.toBytes())
                if (!started) {
                    writeContinuation = null
                    cont.resume(false)
                }
            }
        } ?: run {
            Log.w(TAG, "写入超时")
            writeContinuation = null
            false
        }
    }

    /**
     * Enables BLE Notification on the target characteristic so the remote peripheral
     * can push data to [packets].
     *
     * Uses [BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE], consistent with
     * [BLEServer] which listens for the same value in its descriptor-write handler.
     */
    fun subscribeToNotifications(): Boolean {
        val g = gatt ?: return false
        val c = characteristic ?: return false

        if (!g.setCharacteristicNotification(c, true)) return false

        val descriptor = c.getDescriptor(BLEConfig.DESCRIPTOR_UUID) ?: return false
        return writeDescriptorCompat(
            g,
            descriptor
        )
    }

    fun close() {
        job.cancel()           // cancels all coroutines in this connection's scope
        connectContinuation?.cancel()
        writeContinuation?.cancel()
        closeGatt()
    }

    private fun closeGatt() {
        gatt?.close()
        gatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "已连接，请求 MTU: ${BLEConfig.MTU_SIZE}")
                    gatt.requestMtu(BLEConfig.MTU_SIZE)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "连接断开 (status=$status)")
                    closeGatt()
                    connectContinuation?.resume(false)
                    connectContinuation = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "MTU 协商完成: $mtu (status=$status)")
            // Proceed regardless of MTU negotiation outcome
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val found = status == BluetoothGatt.GATT_SUCCESS &&
                    gatt.getService(BLEConfig.SERVICE_UUID)
                        ?.getCharacteristic(BLEConfig.CHARACTERISTIC_UUID)
                        ?.also { characteristic = it } != null

            Log.d(TAG, "服务发现${if (found) "成功" else "失败"} (status=$status)")
            connectContinuation?.resume(found)
            connectContinuation = null
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            val success = status == BluetoothGatt.GATT_SUCCESS
            if (!success) Log.w(TAG, "写入失败 (status=$status)")
            writeContinuation?.resume(success)
            writeContinuation = null
        }

        // Android 13+
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) = handleNotification(value)

        // Android < 13
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) = handleNotification(characteristic.value)

        private fun handleNotification(value: ByteArray) {
            runCatching { BLEPacket.fromBytes(value) }
                .onSuccess { packet -> scope.launch { _packets.emit(packet) } }
                .onFailure { Log.e(TAG, "解析 BlePacket 失败: ${it.message}") }
        }
    }

    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        char: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeCharacteristic(
            char, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ) == BluetoothStatusCodes.SUCCESS
    } else {
        @Suppress("DEPRECATION")
        char.value = value
        @Suppress("DEPRECATION")
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        @Suppress("DEPRECATION")
        gatt.writeCharacteristic(char)
    }

    private fun writeDescriptorCompat(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
    ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
    } else {
        @Suppress("DEPRECATION")
        descriptor.value = value
        @Suppress("DEPRECATION")
        gatt.writeDescriptor(descriptor)
    }
}