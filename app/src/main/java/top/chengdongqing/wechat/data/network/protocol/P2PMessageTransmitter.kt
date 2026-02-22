package top.chengdongqing.wechat.data.network.protocol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.service.modules.BLEModule
import javax.inject.Inject

/**
 * 基于 BLE (低功耗蓝牙) 的点对点消息传输
 *
 * 1. JSON 消息传输
 * 2. 二进制数据传输(可选)
 * 3. 分阶段传输: 先发送 JSON 元数据,再发送二进制数据
 */
class P2PMessageTransmitter @Inject constructor(
    private val bleDiscovery: BLEDiscovery,
    private val json: Json
) {

    private companion object {
        const val TAG = "P2PMessageTransmitter"

        // 延迟配置
        const val DELAY_BEFORE_BINARY_MS = 200L  // JSON 发送后等待
        const val DELAY_BEFORE_CLOSE_MS = 500L   // 关闭连接前等待
    }

    /**
     * 发送消息到目标设备
     *
     * @param targetUserId 目标用户 ID
     * @param message P2P 消息对象
     * @param binaryData 可选的二进制数据(如图片、文件等)
     * @return 是否发送成功
     *
     * 发送流程:
     * 1. 扫描并连接目标设备
     * 2. 发送 JSON 消息
     * 3. (可选) 发送二进制数据
     * 4. 关闭连接
     */
    @SuppressLint("MissingPermission")
    suspend fun sendMessage(
        targetUserId: String,
        message: P2PMessage,
        binaryData: ByteArray? = null
    ): Boolean {
        var gatt: BluetoothGatt? = null

        return try {
            // 1. 连接到目标设备
            gatt = connectToDevice(targetUserId) ?: return false

            // 2. 获取通信特征
            val characteristic = findCharacteristic(gatt) ?: run {
                gatt.close()
                return false
            }

            // 3. 发送 JSON 消息
            if (!sendJsonMessage(gatt, characteristic, message)) {
                gatt.close()
                return false
            }

            // 4. 发送二进制数据(如果有)
            if (binaryData != null) {
                if (!sendBinaryData(gatt, characteristic, binaryData)) {
                    gatt.close()
                    return false
                }
            }

            // 5. 等待数据传输完成后关闭连接
            delay(DELAY_BEFORE_CLOSE_MS)
            gatt.close()
            Log.d(TAG, "✅ 消息发送完成")
            true

        } catch (e: Exception) {
            Log.e(TAG, "发送消息异常", e)
            gatt?.close()
            false
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 连接到目标设备
     */
    @SuppressLint("MissingPermission")
    private suspend fun connectToDevice(targetUserId: String): BluetoothGatt? {
        val deviceId = targetUserId.toMD5Hex()
        Log.d(TAG, "正在连接设备: userId=$targetUserId, deviceId=$deviceId")

        val gatt = bleDiscovery.scanAndConnect(deviceId)
        if (gatt == null) {
            Log.e(TAG, "连接设备失败: $targetUserId")
        } else {
            Log.d(TAG, "设备连接成功: $targetUserId")
        }

        return gatt
    }

    /**
     * 查找通信特征
     */
    @SuppressLint("MissingPermission")
    private fun findCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
        val service = gatt.getService(BLEModule.SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "未找到服务: ${BLEModule.SERVICE_UUID}")
            return null
        }

        val characteristic = service.getCharacteristic(BLEModule.CHARACTERISTIC_UUID)
        if (characteristic == null) {
            Log.e(TAG, "未找到特征: ${BLEModule.CHARACTERISTIC_UUID}")
        }

        return characteristic
    }

    /**
     * 发送 JSON 消息
     */
    @SuppressLint("MissingPermission")
    private suspend fun sendJsonMessage(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        message: P2PMessage
    ): Boolean {
        // 使用多态序列化将 P2PMessage 及其子类序列化为 JSON
        val messageJson = json.encodeToString<P2PMessage>(message)
        val messageBytes = messageJson.toByteArray(Charsets.UTF_8)

        Log.d(TAG, "发送 JSON: type=${message::class.simpleName}, size=${messageBytes.size} bytes")

        val success = bleDiscovery.writeCharacteristic(gatt, characteristic, messageBytes)

        if (success) {
            Log.d(TAG, "✅ JSON 发送成功")
        } else {
            Log.e(TAG, "❌ JSON 发送失败")
        }

        return success
    }

    /**
     * 发送二进制数据
     */
    @SuppressLint("MissingPermission")
    private suspend fun sendBinaryData(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        binaryData: ByteArray
    ): Boolean {
        // 等待 JSON 传输完成
        delay(DELAY_BEFORE_BINARY_MS)

        Log.d(TAG, "发送二进制数据: size=${binaryData.size} bytes")

        val success = bleDiscovery.writeCharacteristic(gatt, characteristic, binaryData)

        if (success) {
            Log.d(TAG, "✅ 二进制数据发送成功")
        } else {
            Log.e(TAG, "❌ 二进制数据发送失败")
        }

        return success
    }
}