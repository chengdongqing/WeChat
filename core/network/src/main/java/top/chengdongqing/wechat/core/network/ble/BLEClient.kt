package top.chengdongqing.wechat.core.network.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * BLE Central (client) role — scanning only.
 *
 * Scans for a BLE peripheral advertising [BLEConfig.SERVICE_UUID] whose service data
 * matches the leading 4 bytes (8 hex chars) of targetUserIdHash
 *
 * Connection lifecycle is handled separately by [BLEConnectionManager].
 */
@Singleton
@SuppressLint("MissingPermission")
class BLEClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "BLEClient"
    }

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    /**
     * Scans for a device advertising the given [targetUserIdHash] (full MD5 hex string).
     *
     * Internally compares only the first 8 hex characters (4 bytes) against the advertised
     * service data, matching the [BLEConfig.USER_ID_HASH_LENGTH] used by [BLEServer].
     *
     * @return the matched [BluetoothDevice], or null on timeout / scan failure.
     */
    suspend fun scanForDevice(targetUserIdHash: String): BluetoothDevice? {
        val adapter = bluetoothAdapter?.takeIf { it.isEnabled } ?: run {
            Log.e(TAG, "蓝牙不可用")
            return null
        }
        val scanner = adapter.bluetoothLeScanner ?: run {
            Log.e(TAG, "BLE 扫描器不可用")
            return null
        }

        val targetHash = targetUserIdHash.take(8) // first 4 bytes as hex

        return withTimeoutOrNull(BLEConfig.SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val filter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(BLEConfig.SERVICE_UUID))
                    .build()

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    .setReportDelay(0)
                    .build()

                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val serviceData = result.scanRecord
                            ?.getServiceData(ParcelUuid(BLEConfig.SERVICE_UUID)) ?: return

                        if (serviceData.toHexString() == targetHash) {
                            scanner.stopScan(this)
                            if (cont.isActive) cont.resume(result.device)
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "扫描失败，错误码: $errorCode")
                        if (cont.isActive) cont.resume(null)
                    }
                }

                scanner.startScan(listOf(filter), settings, callback)

                cont.invokeOnCancellation {
                    scanner.stopScan(callback)
                }
            }
        } ?: run {
            Log.w(TAG, "扫描超时")
            null
        }
    }
}